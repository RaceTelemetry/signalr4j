/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package tel.race.signalr.client;

import java.util.Map.Entry;

/**
 * Simple Entry&lt;K,V&gt; implementation
 *
 * @param <K> Key
 * @param <V> Value
 */
public class SimpleEntry<K, V> implements Entry<K, V> {
    K key;
    V value;

    /**
     * Initializes the SimpleEntry
     *
     * @param key   Entry key
     * @param value Entry value
     */
    public SimpleEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        this.value = value;
        return this.value;
    }
}