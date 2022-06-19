/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client;

import tel.race.signalr.client.http.HttpConnection;

public interface PlatformComponent {
    /**
     * Returns a platform-specific HttpConnection
     *
     * @return connection
     */
    public HttpConnection createHttpConnection();

    /**
     * Returns a platform-specific Operating System name
     *
     * @return operating system
     */
    public String getOSName();

    boolean useProxy();

    String getProxyHost();

    int getProxyPort();

}