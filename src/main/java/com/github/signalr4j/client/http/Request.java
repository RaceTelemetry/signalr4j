/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.http;

import com.github.signalr4j.client.Constants;
import com.github.signalr4j.client.SimpleEntry;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents an HTTP Request
 */
public class Request {

    private String verb;

    private String content;

    private HashMap<String, String> headers = new HashMap<>();

    private String url;

    /**
     * Initializes a request with an HTTP verb
     *
     * @param httpVerb the HTTP verb
     */
    public Request(String httpVerb) {
        verb = httpVerb;
    }

    /**
     * Sets the request content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns the request content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the request content with a single name-value pair, using form
     * encoding
     *
     * @param name  The name for the form data
     * @param value The value for the form data
     */
    public void setFormContent(String name, String value) {
        List<Entry<String, String>> formValues = new ArrayList<>();
        formValues.add(new SimpleEntry<>(name, value));

        setFormContent(formValues);
    }

    /**
     * Sets the request content with several name-value pairs, using form
     * encoding
     *
     * @param formValues The name-value pairs
     */
    public void setFormContent(List<Entry<String, String>> formValues) {
        StringBuilder sb = new StringBuilder();

        for (Entry<String, String> entry : formValues) {
            try {
                sb.append(String.format("%s=%s&", URLEncoder.encode(entry.getKey(), Constants.UTF8_NAME),
                        URLEncoder.encode(entry.getValue(), Constants.UTF8_NAME)));
            } catch (UnsupportedEncodingException ignored) {
            }
        }

        content = sb.toString();
    }

    /**
     * Returns the request headers
     */
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    /**
     * Returns the value of the named header field. If called on a connection
     * that sets the same header multiple times with possibly different values,
     * only the last value is returned.
     *
     * @param name the name of a header field.
     * @return the value of the named header field, or null if there is no such
     * field in the header.
     */
    public String getHeaderField(String name) {
        return this.headers.getOrDefault(name, null);
    }

    /**
     * Sets the request headers
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = new HashMap<>();

        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    /**
     * Adds a header to the request
     *
     * @param name  The header name
     * @param value The header value
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Removes a header
     *
     * @param name The header name
     */
    public void removeHeader(String name) {
        headers.remove(name);
    }

    /**
     * Sets the request HTTP verb
     */
    public void setVerb(String httpVerb) {
        verb = httpVerb;
    }

    /**
     * Returns the request HTTP verb
     */
    public String getVerb() {
        return verb;
    }

    /**
     * Sets the request URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the request URL
     */
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "Request(" +
                ", url='" + url + '\'' +
                "verb='" + verb + '\'' +
                ", headers=" + headers +
                ", content='" + content + '\'' +
                ')';
    }
}
