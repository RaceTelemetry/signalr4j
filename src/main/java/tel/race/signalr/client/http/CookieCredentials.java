/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.http;

import tel.race.signalr.client.Constants;
import tel.race.signalr.client.Credentials;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents credentials based on cookie values
 */
public class CookieCredentials implements Credentials {

    private Map<String, String> cookieValues;

    /**
     * Creates a new instance
     */
    public CookieCredentials() {
        cookieValues = new HashMap<String, String>();
    }

    public CookieCredentials(String cookie) {
        cookieValues = new HashMap<String, String>();

        if (cookie != null) {
            cookie = cookie.trim();

            if (!cookie.trim().equals("")) {
                String[] keyValues = cookie.split(";");
                for (int i = 0; i < keyValues.length; i++) {
                    String[] parts = keyValues[i].split("=");
                    try {
                        addCookie(URLDecoder.decode(parts[0], Constants.UTF8_NAME), URLDecoder.decode(parts[1], Constants.UTF8_NAME));
                    } catch (UnsupportedEncodingException e) {
                    }
                }
            }
        }
    }

    /**
     * Adds a cookie to the credential
     *
     * @param name  The cookie name
     * @param value The cookie value
     */
    public void addCookie(String name, String value) {
        cookieValues.put(name, value);
    }

    /**
     * Removes a cookie from the credential
     *
     * @param name The cookie name
     */
    public void removeCookie(String name) {
        cookieValues.remove(name);
    }

    @Override
    public void prepareRequest(Request request) {
        if (cookieValues.size() > 0) {
            StringBuilder currentCookies = new StringBuilder();
            if (request.getHeaders().containsKey("Cookie")) {
                currentCookies.append(request.getHeaders().get("Cookie"));

                currentCookies.append("; ");
            }

            currentCookies.append(this.toString());

            request.removeHeader("Cookie");
            request.addHeader("Cookie", currentCookies.toString());
        }
    }

    @Override
    public String toString() {
        if (cookieValues.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (String key : cookieValues.keySet()) {
                try {
                    sb.append(URLEncoder.encode(key, Constants.UTF8_NAME));
                    sb.append("=");
                    sb.append(URLEncoder.encode(cookieValues.get(key), Constants.UTF8_NAME));
                    sb.append(";");
                } catch (UnsupportedEncodingException ignored) {
                }
            }

            return sb.toString();
        } else {
            return "";
        }
    }
}