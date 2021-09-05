/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response implementation based on an InputStream
 */
public class StreamResponse implements Response {
    private final BufferedReader reader;
    private final int status;
    private final InputStream originalStream;
    Map<String, List<String>> headers;

    /**
     * Initializes the StreamResponse
     *
     * @param stream stream to read
     * @param status HTTP status code
     */
    public StreamResponse(InputStream stream, int status, Map<String, List<String>> headers) {
        originalStream = stream;
        reader = new BufferedReader(new InputStreamReader(originalStream, StandardCharsets.UTF_8));
        this.headers = new HashMap<>(headers);
        this.status = status;
    }

    public byte[] readAllBytes() throws IOException {
        List<Byte> bytes = new ArrayList<>();

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int bytesRead = originalStream.read(buffer, 0, bufferSize);
        while (bytesRead != -1) {
            for (int i = 0; i < bytesRead; i++) {
                bytes.add(buffer[i]);
            }

            bytesRead = originalStream.read(buffer, 0, bufferSize);
        }

        byte[] byteArray = new byte[bytes.size()];

        for (int i = 0; i < bytes.size(); i++) {
            byteArray[i] = bytes.get(i);
        }

        return byteArray;
    }

    @Override
    public String readToEnd() throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return new HashMap<>(headers);
    }

    @Override
    public List<String> getHeader(String headerName) {
        return headers.get(headerName);
    }
}
