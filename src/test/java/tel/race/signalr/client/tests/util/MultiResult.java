/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client.tests.util;

import tel.race.signalr.client.ConnectionState;
import tel.race.signalr.client.SignalRFuture;

import java.util.ArrayList;
import java.util.List;

public class MultiResult {
    public boolean booleanResult = false;
    public int intResult = 0;
    public String stringResult = null;
    public SignalRFuture<?> futureResult = null;
    public List<Object> listResult = new ArrayList<Object>();
    public List<Throwable> errorsResult = new ArrayList<Throwable>();
    public List<ConnectionState> statesResult = new ArrayList<ConnectionState>();
}
