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
    static PlatformComponent mPlatformComponent = null;

    public static void loadPlatformComponent(PlatformComponent platformComponent) {
        mPlatformComponent = platformComponent;
    }

    /**
     * Creates an adequate HttpConnection for the current platform
     *
     * @param logger
     *            Logger to use with the connection
     * @return An HttpConnection
     */
    public static HttpConnection createHttpConnection(Logger logger) {
        if (mPlatformComponent != null) {
            return mPlatformComponent.createHttpConnection(logger);
        } else {
            return createDefaultHttpConnection(logger);
        }
    }

    public static HttpConnection createDefaultHttpConnection(Logger logger) {
        return new JavaHttpConnection(logger);
    }

    /**
     * Generates the User-Agent
     */
    public static String getUserAgent() {
        String osName;

        if (mPlatformComponent != null) {
            osName = mPlatformComponent.getOSName();
        } else {
            osName = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        }

        return String.format("SignalR (lang=Java; os=%s; version=2.0)", osName);
    }

    public static boolean useProxy() {
        return (mPlatformComponent != null) && mPlatformComponent.useProxy();
    }

    public static String getProxyHost() {
        return (mPlatformComponent != null) ? mPlatformComponent.getProxyHost() : null;
    }

    public static int getProxyPort() {
        return (mPlatformComponent != null) ? mPlatformComponent.getProxyPort() : -1;
    }

}
