/*
 * Copyright (C) 2015 Square, Inc, 2017 Jeff Gilfelt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.readystatesoftware.chuck;

import android.net.Uri;
import android.util.Log;

import com.readystatesoftware.chuck.internal.data.HttpTransaction;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;

/**
 * An OkHttp Interceptor which persists and displays HTTP activity in your application for later inspection.
 */
public abstract class Chuck2Interceptor implements Interceptor {


    private static final String LOG_TAG = "ChuckInterceptor";
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private long maxContentLength = 250000L;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        HttpTransaction transaction = getHttpTransaction(request);
        Uri transactionUri = create(transaction);

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            transaction.setError(e.toString());
            update(transaction, transactionUri);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        doAfterResponse(transaction, transactionUri, response, tookMs);
        return response;
    }



    private HttpTransaction getHttpTransaction(Request request) throws IOException {

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        HttpTransaction transaction = new HttpTransaction();
        transaction.setRequestDate(new Date());

        transaction.setMethod(request.method());
        transaction.setUrl(request.url().toString());

        transaction.setRequestHeaders(request.headers());
        if (hasRequestBody) {
            if (requestBody.contentType() != null) {
                transaction.setRequestContentType(requestBody.contentType().toString());
            }
            if (requestBody.contentLength() != -1) {
                transaction.setRequestContentLength(requestBody.contentLength());
            }
        }

        transaction.setRequestBodyIsPlainText(!bodyHasUnsupportedEncoding(request.headers()));
        if (hasRequestBody && transaction.requestBodyIsPlainText()) {
            BufferedSource source = getNativeSource(new Buffer(), bodyGzipped(request.headers()));
            Buffer buffer = source.buffer();
            requestBody.writeTo(buffer);
            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            if (isPlaintext(buffer)) {
                transaction.setRequestBody(readFromBuffer(buffer, charset));
            } else {
                transaction.setResponseBodyIsPlainText(false);
            }
        }
        return transaction;
    }

    /**
     * 对应答后的报文进行处理
     *
     * @param transaction
     * @param transactionUri
     * @param response
     * @param tookMs
     * @return true-处理成功，Response的contentType.charset(UTF8)，设置成功；false-设置失败
     * @throws IOException
     */
    private void doAfterResponse(HttpTransaction transaction, Uri transactionUri, Response response, long tookMs) throws IOException {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return;
        }

        // includes headers added later in the chain
        transaction.setRequestHeaders(response.request().headers());
        transaction.setResponseDate(new Date());
        transaction.setTookMs(tookMs);
        transaction.setProtocol(response.protocol().toString());
        transaction.setResponseCode(response.code());
        transaction.setResponseMessage(response.message());

        transaction.setResponseContentLength(responseBody.contentLength());
        if (responseBody.contentType() != null) {
            transaction.setResponseContentType(responseBody.contentType().toString());
        }
        transaction.setResponseHeaders(response.headers());

        transaction.setResponseBodyIsPlainText(!bodyHasUnsupportedEncoding(response.headers()));
        if (HttpHeaders.hasBody(response) && transaction.responseBodyIsPlainText()) {
            BufferedSource source = getNativeSource(response);
            if (source != null) {
                source.request(Long.MAX_VALUE);
                Buffer buffer = source.buffer();
                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8);
                    } catch (UnsupportedCharsetException e) {
                        update(transaction, transactionUri);
                        return;
                    }
                }
                if (isPlaintext(buffer)) {
                    transaction.setResponseBody(readFromBuffer(buffer.clone(), charset));
                } else {
                    transaction.setResponseBodyIsPlainText(false);
                }
                transaction.setResponseContentLength(buffer.size());
            }
        }
        update(transaction, transactionUri);
    }

    /**
     * IPC写事物
     * @param transaction
     * @return
     */
    protected abstract Uri create(HttpTransaction transaction);

    /**
     * IPC更新事物
     * @param transaction
     * @param uri
     * @return
     */
    protected abstract int update(HttpTransaction transaction, Uri uri);

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            // Truncated UTF-8 sequence.
            return false;
        }
    }

    private boolean bodyHasUnsupportedEncoding(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null &&
                !"identity".equalsIgnoreCase(contentEncoding) &&
                !"gzip".equalsIgnoreCase(contentEncoding);
    }

    private boolean bodyGzipped(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    private String readFromBuffer(Buffer buffer, Charset charset) {
        long bufferSize = buffer.size();
        long maxBytes = Math.min(bufferSize, maxContentLength);
        String body = "";
        try {
            body = buffer.readString(maxBytes, charset);
        } catch (EOFException e) {
            body += "\n\n--- Unexpected end of content ---";
        }
        if (bufferSize > maxContentLength) {
            body += "\n\n--- Content truncated ---";
        }
        return body;
    }

    private BufferedSource getNativeSource(BufferedSource input, boolean isGzipped) {
        if (isGzipped) {
            GzipSource source = new GzipSource(input);
            return Okio.buffer(source);
        } else {
            return input;
        }
    }

    private BufferedSource getNativeSource(Response response) throws IOException {
        if (bodyGzipped(response.headers())) {
            BufferedSource source = response.peekBody(maxContentLength).source();
            if (source.buffer().size() < maxContentLength) {
                return getNativeSource(source, true);
            } else {
                Log.w(LOG_TAG, "gzip encoded response was too long");
            }
        }
        if (response.body() != null) {
            return response.body().source();
        }
        return null;
    }
}
