/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client;

import com.github.signalr4j.client.http.HttpConnection;
import com.github.signalr4j.client.http.java.JavaHttpConnection;

import java.util.Locale;

/**
 * Platform specific classes and operations
 */
public class Platform {
    static boolean PLATFORM_VERIFIED = false;
    static boolean IS_ANDROID = false;
    static PlatformComponent platformComponent = null;

    public static void loadPlatformComponent(PlatformComponent platformComponent) {
        Platform.platformComponent = platformComponent;
    }

    /**
     * Creates an adequate HttpConnection for the current platform
     *
     * @return An HttpConnection
     */
    public static HttpConnection createHttpConnection() {
        if (platformComponent != null) {
            return platformComponent.createHttpConnection();
        } else {
            return createDefaultHttpConnection();
        }
    }

    public static HttpConnection createDefaultHttpConnection() {
        return new JavaHttpConnection();
    }

    /**
     * Generates the User-Agent
     */
    public static String getUserAgent() {
        String osName;

        if (platformComponent != null) {
            osName = platformComponent.getOSName();
        } else {
            osName = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        }

        return String.format("SignalR (lang=Java; os=%s; version=2.0)", osName);
    }

    public static boolean useProxy() {
        return (platformComponent != null) && platformComponent.useProxy();
    }

    public static String getProxyHost() {
        return (platformComponent != null) ? platformComponent.getProxyHost() : null;
    }

    public static int getProxyPort() {
        return (platformComponent != null) ? platformComponent.getProxyPort() : -1;
    }

}
