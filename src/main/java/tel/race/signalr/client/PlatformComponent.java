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
     */
    public HttpConnection createHttpConnection();

    /**
     * Returns a platform-specific Operating System name
     */
    public String getOSName();

    boolean useProxy();

    String getProxyHost();

    int getProxyPort();

}
