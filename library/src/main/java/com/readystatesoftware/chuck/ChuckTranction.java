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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.readystatesoftware.chuck.internal.data.ChuckContentProvider;
import com.readystatesoftware.chuck.internal.data.HttpTransaction;
import com.readystatesoftware.chuck.internal.data.LocalCupboard;
import com.readystatesoftware.chuck.internal.support.NotificationHelper;
import com.readystatesoftware.chuck.internal.support.RetentionManager;

/**
 * An OkHttp Interceptor which persists and displays HTTP activity in your application for later inspection.
 */
public final class ChuckTranction {

    private static final String LOG_TAG = "ChuckTranction";
    private static final ChuckInterceptor.Period DEFAULT_RETENTION = ChuckInterceptor.Period.ONE_WEEK;
    private final Context context;
    private final NotificationHelper notificationHelper;
    private RetentionManager retentionManager;
    private boolean showNotification;

    /**
     * @param context The current Context.
     */
    public ChuckTranction(Context context) {
        this.context = context.getApplicationContext();
        notificationHelper = new NotificationHelper(this.context);
        showNotification = true;
        retentionManager = new RetentionManager(this.context, DEFAULT_RETENTION);
    }

    /**
     * Control whether a notification is shown while HTTP activity is recorded.
     *
     * @param show true to show a notification, false to suppress it.
     * @return The {@link ChuckTranction} instance.
     */
    public ChuckTranction showNotification(boolean show) {
        showNotification = show;
        return this;
    }
  
    /**
     * Set the retention period for HTTP transaction data captured by this interceptor.
     * The default is one week.
     *
     * @param period the peroid for which to retain HTTP transaction data.
     * @return The {@link ChuckTranction} instance.
     */
    public ChuckTranction retainDataFor(ChuckInterceptor.Period period) {
        retentionManager = new RetentionManager(context, period);
        return this;
    }

    public Uri create(HttpTransaction transaction) {
        ContentValues values = LocalCupboard.getInstance().withEntity(HttpTransaction.class).toContentValues(transaction);
        Uri uri = context.getContentResolver().insert(ChuckContentProvider.TRANSACTION_URI, values);
        Long transactionId = getTransactionId(uri);
        if (transactionId != null) {
            transaction.setId(transactionId);
        }
        if (showNotification) {
            notificationHelper.show(transaction);
        }
        retentionManager.doMaintenance();
        return uri;
    }

    public static Long getTransactionId(Uri uri) {
        Long id = null;
        if (uri != null) {
            String lastPathSegment = uri.getLastPathSegment();
            try {
                if (lastPathSegment != null) {
                    id = Long.parseLong(lastPathSegment);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    public int update(HttpTransaction transaction, Uri uri) {
        Long transactionId = getTransactionId(uri);
        if (transactionId == null) {
            return 0;
        }
        transaction.setId(transactionId);
        ContentValues values = LocalCupboard.getInstance().withEntity(HttpTransaction.class).toContentValues(transaction);
        int updated = context.getContentResolver().update(uri, values, null, null);
        if (showNotification && updated > 0) {
            notificationHelper.show(transaction);
        }
        return updated;
    }
}
