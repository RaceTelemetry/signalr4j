/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.util;

import com.github.signalr4j.client.http.HttpConnection;
import com.github.signalr4j.client.http.HttpConnectionFuture;
import com.github.signalr4j.client.http.HttpConnectionFuture.ResponseCallback;
import com.github.signalr4j.client.http.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class MockHttpConnection implements HttpConnection {

    Semaphore semaphore = new Semaphore(0);

    Queue<RequestEntry> requests = new ConcurrentLinkedQueue<>();
    List<Thread> threads = new ArrayList<>();

    @Override
    public HttpConnectionFuture execute(Request request, ResponseCallback responseCallback) {
        RequestEntry entry = new RequestEntry();
        entry.request = request;
        entry.callback = responseCallback;
        entry.future = new HttpConnectionFuture();
        entry.response = new MockResponse(200);

        requests.add(entry);
        semaphore.release();

        return entry.future;
    }

    public class RequestEntry {
        public Request request;
        public ResponseCallback callback;
        public HttpConnectionFuture future;
        public MockResponse response;
        private boolean mResponseTriggered = false;
        private final Object mSync = new Object();

        public void finishRequest() {
            Thread t = new Thread(() -> {
                response.finishWriting();
                future.setResult(null);
            });

            threads.add(t);

            t.start();
        }

        public void triggerResponse() {
            synchronized (mSync) {
                if (!mResponseTriggered) {
                    mResponseTriggered = true;

                    Thread t = new Thread(() -> {
                        try {
                            callback.onResponse(response);
                        } catch (Exception ignored) {
                        }
                    });

                    threads.add(t);

                    t.start();
                }
            }
        }
    }

    public RequestEntry getRequest() throws InterruptedException {
        semaphore.acquire();
        return requests.poll();
    }
}
