/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.realtransport;

import tel.race.signalr.client.tests.util.TransportType;

public class ServerSentEventsTransportTests extends HttpClientTransportTests {

    @Override
    protected TransportType getTransportType() {
        return TransportType.SERVER_SENT_EVENTS;
    }

}