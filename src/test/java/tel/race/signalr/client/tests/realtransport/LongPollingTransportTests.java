/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.realtransport;

import org.junit.Before;
import tel.race.signalr.client.tests.util.Sync;
import tel.race.signalr.client.tests.util.TransportType;

public class LongPollingTransportTests extends HttpClientTransportTests {

    @Before
    public void setUp() {
        Sync.reset();
    }


    @Override
    protected TransportType getTransportType() {
        return TransportType.LONG_POLLING;
    }

}