/*
 * Copyright (C) 2017 Jeff Gilfelt.
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
package com.readystatesoftware.chuck.internal.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.reflect.TypeToken;
import com.readystatesoftware.chuck.internal.support.FormatUtils;
import com.readystatesoftware.chuck.internal.support.JsonConvertor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.qbusict.cupboard.annotation.Index;
import okhttp3.Headers;

public class HttpTransaction implements Parcelable {

    public HttpTransaction() {
    }

    protected HttpTransaction(Parcel in) {
        if (in.readByte() == 0) {
            _id = null;
        } else {
            _id = in.readLong();
        }
        if (in.readByte() == 0) {
            tookMs = null;
        } else {
            tookMs = in.readLong();
        }
        protocol = in.readString();
        method = in.readString();
        url = in.readString();
        host = in.readString();
        path = in.readString();
        scheme = in.readString();
        if (in.readByte() == 0) {
            requestContentLength = null;
        } else {
            requestContentLength = in.readLong();
        }
        requestContentType = in.readString();
        requestHeaders = in.readString();
        requestBody = in.readString();
        requestBodyIsPlainText = in.readByte() != 0;
        if (in.readByte() == 0) {
            responseCode = null;
        } else {
            responseCode = in.readInt();
        }
        responseMessage = in.readString();
        error = in.readString();
        if (in.readByte() == 0) {
            responseContentLength = null;
        } else {
            responseContentLength = in.readLong();
        }
        responseContentType = in.readString();
        responseHeaders = in.readString();
        responseBody = in.readString();
        responseBodyIsPlainText = in.readByte() != 0;
    }

    public static final Creator<HttpTransaction> CREATOR = new Creator<HttpTransaction>() {
        @Override
        public HttpTransaction createFromParcel(Parcel in) {
            return new HttpTransaction(in);
        }

        @Override
        public HttpTransaction[] newArray(int size) {
            return new HttpTransaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (_id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(_id);
        }
        if (tookMs == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(tookMs);
        }
        dest.writeString(protocol);
        dest.writeString(method);
        dest.writeString(url);
        dest.writeString(host);
        dest.writeString(path);
        dest.writeString(scheme);
        if (requestContentLength == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(requestContentLength);
        }
        dest.writeString(requestContentType);
        dest.writeString(requestHeaders);
        dest.writeString(requestBody);
        dest.writeByte((byte) (requestBodyIsPlainText ? 1 : 0));
        if (responseCode == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(responseCode);
        }
        dest.writeString(responseMessage);
        dest.writeString(error);
        if (responseContentLength == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(responseContentLength);
        }
        dest.writeString(responseContentType);
        dest.writeString(responseHeaders);
        dest.writeString(responseBody);
        dest.writeByte((byte) (responseBodyIsPlainText ? 1 : 0));
    }

    public enum Status {
        Requested,
        Complete,
        Failed
    }

    public static final String[] PARTIAL_PROJECTION = new String[] {
            "_id",
            "requestDate",
            "tookMs",
            "method",
            "host",
            "path",
            "scheme",
            "requestContentLength",
            "responseCode",
            "error",
            "responseContentLength"
    };

    private static final SimpleDateFormat TIME_ONLY_FMT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private Long _id;
    @Index private Date requestDate;
    private Date responseDate;
    private Long tookMs;

    private String protocol;
    private String method;
    private String url;
    private String host;
    private String path;
    private String scheme;

    private Long requestContentLength;
    private String requestContentType;
    private String requestHeaders;
    private String requestBody;
    private boolean requestBodyIsPlainText = true;

    private Integer responseCode;
    private String responseMessage;
    private String error;

    private Long responseContentLength;
    private String responseContentType;
    private String responseHeaders;
    private String responseBody;
    private boolean responseBodyIsPlainText = true;

    public Long getId() {
        return _id;
    }

    public void setId(long id) {
        _id = id;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getFormattedRequestBody() {
        return formatBody(requestBody, requestContentType);
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public boolean requestBodyIsPlainText() {
        return requestBodyIsPlainText;
    }

    public void setRequestBodyIsPlainText(boolean requestBodyIsPlainText) {
        this.requestBodyIsPlainText = requestBodyIsPlainText;
    }

    public Long getRequestContentLength() {
        return requestContentLength;
    }

    public void setRequestContentLength(Long requestContentLength) {
        this.requestContentLength = requestContentLength;
    }

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(String requestContentType) {
        this.requestContentType = requestContentType;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getFormattedResponseBody() {
        return formatBody(responseBody, responseContentType);
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public boolean responseBodyIsPlainText() {
        return responseBodyIsPlainText;
    }

    public void setResponseBodyIsPlainText(boolean responseBodyIsPlainText) {
        this.responseBodyIsPlainText = responseBodyIsPlainText;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public Long getResponseContentLength() {
        return responseContentLength;
    }

    public void setResponseContentLength(Long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Long getTookMs() {
        return tookMs;
    }

    public void setTookMs(Long tookMs) {
        this.tookMs = tookMs;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        Uri uri = Uri.parse(url);
        host = uri.getHost();
        path = uri.getPath() + ((uri.getQuery() != null) ? "?" + uri.getQuery() : "");
        scheme = uri.getScheme();
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public String getScheme() {
        return scheme;
    }

    public void setRequestHeaders(Headers headers) {
        setRequestHeaders(toHttpHeaderList(headers));
    }

    public void setRequestHeaders(List<HttpHeader> headers) {
        requestHeaders = JsonConvertor.getInstance().toJson(headers);
    }

    public List<HttpHeader> getRequestHeaders() {
        return JsonConvertor.getInstance().fromJson(requestHeaders,
                new TypeToken<List<HttpHeader>>(){}.getType());
    }

    public String getRequestHeadersString(boolean withMarkup) {
        return FormatUtils.formatHeaders(getRequestHeaders(), withMarkup);
    }

    public void setResponseHeaders(Headers headers) {
        setResponseHeaders(toHttpHeaderList(headers));
    }

    public void setResponseHeaders(List<HttpHeader> headers) {
        responseHeaders = JsonConvertor.getInstance().toJson(headers);
    }

    public List<HttpHeader> getResponseHeaders() {
        return JsonConvertor.getInstance().fromJson(responseHeaders,
                new TypeToken<List<HttpHeader>>(){}.getType());
    }

    public String getResponseHeadersString(boolean withMarkup) {
        return FormatUtils.formatHeaders(getResponseHeaders(), withMarkup);
    }

    public Status getStatus() {
        if (error != null) {
            return Status.Failed;
        } else if (responseCode == null) {
            return Status.Requested;
        } else {
            return Status.Complete;
        }
    }

    public String getRequestStartTimeString() {
        return (requestDate != null) ? TIME_ONLY_FMT.format(requestDate) : null;
    }

    public String getRequestDateString() {
        return (requestDate != null) ? requestDate.toString() : null;
    }

    public String getResponseDateString() {
        return (responseDate != null) ? responseDate.toString() : null;
    }

    public String getDurationString() {
        return (tookMs != null) ? + tookMs + " ms" : null;
    }

    public String getRequestSizeString() {
        return formatBytes((requestContentLength != null) ? requestContentLength : 0);
    }
    public String getResponseSizeString() {
        return (responseContentLength != null) ? formatBytes(responseContentLength) : null;
    }

    public String getTotalSizeString() {
        long reqBytes = (requestContentLength != null) ? requestContentLength : 0;
        long resBytes = (responseContentLength != null) ? responseContentLength : 0;
        return formatBytes(reqBytes + resBytes);
    }

    public String getResponseSummaryText() {
        switch (getStatus()) {
            case Failed:
                return error;
            case Requested:
                return null;
            default:
                return responseCode + " " + responseMessage;
        }
    }

    public String getNotificationText() {
        switch (getStatus()) {
            case Failed:
                return " ! ! !  " + path;
            case Requested:
                return " . . .  " + path;
            default:
                return responseCode + " " + path;
        }
    }

    public boolean isSsl() {
        return "https".equals(scheme.toLowerCase());
    }

    private List<HttpHeader> toHttpHeaderList(Headers headers) {
        List<HttpHeader> httpHeaders = new ArrayList<>();
        for (int i = 0, count = headers.size(); i < count; i++) {
            httpHeaders.add(new HttpHeader(headers.name(i), headers.value(i)));
        }
        return httpHeaders;
    }

    private String formatBody(String body, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("json")) {
            return FormatUtils.formatJson(body);
        } else if (contentType != null && contentType.toLowerCase().contains("xml")) {
            return FormatUtils.formatXml(body);
        } else {
            return body;
        }
    }

    private String formatBytes(long bytes) {
        return FormatUtils.formatByteCount(bytes, true);
    }

    @Override
    public String toString() {
        return "HttpTransaction{" +
                "_id=" + _id +
                ", requestDate=" + requestDate +
                ", responseDate=" + responseDate +
                ", tookMs=" + tookMs +
                ", protocol='" + protocol + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", host='" + host + '\'' +
                ", path='" + path + '\'' +
                ", scheme='" + scheme + '\'' +
                ", requestContentLength=" + requestContentLength +
                ", requestContentType='" + requestContentType + '\'' +
                ", requestHeaders='" + requestHeaders + '\'' +
                ", requestBody='" + requestBody + '\'' +
                ", requestBodyIsPlainText=" + requestBodyIsPlainText +
                ", responseCode=" + responseCode +
                ", responseMessage='" + responseMessage + '\'' +
                ", error='" + error + '\'' +
                ", responseContentLength=" + responseContentLength +
                ", responseContentType='" + responseContentType + '\'' +
                ", responseHeaders='" + responseHeaders + '\'' +
                ", responseBody='" + responseBody + '\'' +
                ", responseBodyIsPlainText=" + responseBodyIsPlainText +
                '}';
    }
}
