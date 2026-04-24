/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.hazelcast.ratelimit;

import io.gravitee.node.api.cluster.DistributedMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Test-only, in-memory implementation of {@link DistributedMap}.
 * Real per-key locking and real TTL expiry without booting Hazelcast.
 * Records the most recent TTL passed to {@link #put(Object, Object, long)} for assertions.
 */
final class InMemoryDistributedMap<K, V> implements DistributedMap<K, V> {

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Map<K, ReentrantLock> locks = new ConcurrentHashMap<>();
    private volatile long lastTtlMillis = -1L;

    @Override
    public V get(K key) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        lastTtlMillis = ttlMillis;
        long expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
        entries.put(key, new Entry<>(value, expiresAt));
    }

    @Override
    public void lock(K key) {
        locks.computeIfAbsent(key, k -> new ReentrantLock()).lock();
    }

    @Override
    public void unlock(K key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    long lastTtlMillis() {
        return lastTtlMillis;
    }

    private record Entry<V>(V value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
