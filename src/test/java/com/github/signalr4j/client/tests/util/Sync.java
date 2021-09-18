/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package com.github.signalr4j.client.tests.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Sync {

    private static Map<String, Semaphore> semaphores = new HashMap<String, Semaphore>();
    public static final Object SYNC = new Object();

    private static Semaphore getSemaphore(String name) {
        synchronized (SYNC) {
            if (!semaphores.containsKey(name)) {
                semaphores.put(name, new Semaphore(0));
            }

            return semaphores.get(name);
        }
    }

    public static void waitComplete(String name, int count) throws InterruptedException {
        getSemaphore(name).acquireUninterruptibly(count);
    }

    public static void waitComplete(String name) throws InterruptedException {
        getSemaphore(name).acquireUninterruptibly();
    }

    public static void complete(String name) {
        getSemaphore(name).release();
    }

    public static void completeAll(String name) {
        getSemaphore(name).release(Integer.MAX_VALUE);
    }

    public static void reset() {
        synchronized (SYNC) {
            semaphores = new HashMap<String, Semaphore>();
        }
    }

}
