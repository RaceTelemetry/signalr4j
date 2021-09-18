/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.util;

import com.github.signalr4j.client.http.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class MockResponse implements Response {

    Semaphore semaphore = new Semaphore(0);

    final Object linesLock = new Object();
    final Queue<String> lines = new ConcurrentLinkedQueue<>();
    Map<String, List<String>> headers = new HashMap<>();
    int status;
    boolean finished = false;

    public MockResponse(int status) {
        this.status = status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void writeLine(String line) {
        if (line != null) {
            synchronized (linesLock) {
                lines.add(line);
            }
            semaphore.release();
        }
    }

    public void finishWriting() {
        finished = true;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = new HashMap<>(headers);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return new HashMap<>(headers);
    }

    @Override
    public List<String> getHeader(String headerName) {
        return headers.get(headerName);
    }

    @Override
    public String readToEnd() {
        StringBuilder sb = new StringBuilder();

        while (!finished || !lines.isEmpty()) {
            String line = readLine();
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public String readLine() {
        if (finished) {
            if (lines.isEmpty()) {
                return null;
            } else {
                synchronized (linesLock) {
                    return lines.poll();
                }
            }
        } else {
            try {
                semaphore.acquire();
            } catch (InterruptedException ignored) {
            }

            synchronized (linesLock) {
                return lines.poll();
            }
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public byte[] readAllBytes() {
        return readToEnd().getBytes();
    }

}
