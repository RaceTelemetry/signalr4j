/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.hubs;

public interface SubscriptionHandler3<E1, E2, E3> {
    void run(E1 p1, E2 p2, E3 p3);
}
