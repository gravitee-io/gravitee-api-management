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
package io.gravitee.gateway.reactive.core.v4.endpoint.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WeightedRoundRobinLoadBalancerTest {

    @Test
    void should_return_null_with_empty_endpoints() {
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(List.of());
        ManagedEndpoint next = cut.next();
        assertThat(next).isNull();
    }

    @Test
    void should_return_endpoint_even_with_invalid_configured_weight() {
        List<ManagedEndpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setWeight(0);
        ManagedEndpoint managedEndpoint1 = new DefaultManagedEndpoint(
            endpoint1,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint1);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        // 1
        ManagedEndpoint next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1); // 1 > 0
    }

    @Test
    void should_return_endpoints_in_order() {
        List<ManagedEndpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setWeight(1);
        ManagedEndpoint managedEndpoint1 = new DefaultManagedEndpoint(
            endpoint1,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint1);
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setWeight(5);
        ManagedEndpoint managedEndpoint2 = new DefaultManagedEndpoint(
            endpoint2,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint2);
        Endpoint endpoint3 = new Endpoint();
        endpoint3.setWeight(3);
        ManagedEndpoint managedEndpoint3 = new DefaultManagedEndpoint(
            endpoint3,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint3);

        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        // 1
        ManagedEndpoint next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1); // 1 > 0
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 5 > 4
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 3 > 2

        // 2
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 4 > 3
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 2 > 1

        // 3
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 3 > 2
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 1 > 0

        // 4
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 2 > 1
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 1 > 0

        // 5
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_not_hang_under_concurrent_load() throws InterruptedException {
        List<ManagedEndpoint> endpoints = buildEndpoints(3, 5, 2);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);

        int threadCount = 20;
        int callsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            ManagedEndpoint result = cut.next();
                            if (result == null) {
                                failed.set(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            boolean completed = executor.awaitTermination(9, TimeUnit.SECONDS);

            assertThat(completed).as("All threads should complete without hanging").isTrue();
            assertThat(failed).as("No thread should encounter a null result or exception").isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_only_return_valid_endpoints_under_concurrent_load() throws InterruptedException {
        List<ManagedEndpoint> endpoints = buildEndpoints(1, 9);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);

        int threadCount = 10;
        int callsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        List<ManagedEndpoint> results = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            ManagedEndpoint result = cut.next();
                            results.add(result);
                        }
                    } catch (Exception e) {
                        // exception will surface as null in results or missing count
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            executor.awaitTermination(9, TimeUnit.SECONDS);

            assertThat(results).hasSize(threadCount * callsPerThread);
            assertThat(results).allMatch(endpoints::contains, "Every result must be one of the configured endpoints");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_handle_concurrent_calls_with_single_endpoint() throws InterruptedException {
        List<ManagedEndpoint> endpoints = buildEndpoints(1);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);

        int threadCount = 10;
        int callsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            ManagedEndpoint result = cut.next();
                            if (result != endpoints.get(0)) {
                                failed.set(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            boolean completed = executor.awaitTermination(9, TimeUnit.SECONDS);

            assertThat(completed).as("All threads should complete without hanging").isTrue();
            assertThat(failed).as("Single endpoint should always be returned").isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_handle_concurrent_calls_with_refresh() throws InterruptedException {
        List<ManagedEndpoint> endpoints = buildEndpoints(3, 5, 2);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);

        int threadCount = 10;
        int callsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            // One thread periodically calls refresh to simulate endpoint changes
                            if (threadId == 0 && i % 50 == 0) {
                                cut.refresh();
                            }
                            ManagedEndpoint result = cut.next();
                            if (result == null || !endpoints.contains(result)) {
                                failed.set(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            boolean completed = executor.awaitTermination(9, TimeUnit.SECONDS);

            assertThat(completed).as("All threads should complete even with concurrent refresh").isTrue();
            assertThat(failed).as("No thread should encounter a null or invalid result").isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    private List<ManagedEndpoint> buildEndpoints(int... weights) {
        List<ManagedEndpoint> endpoints = new ArrayList<>();
        for (int weight : weights) {
            Endpoint endpoint = new Endpoint();
            endpoint.setWeight(weight);
            endpoints.add(
                new DefaultManagedEndpoint(endpoint, new DefaultManagedEndpointGroup(new EndpointGroup()), mock(EndpointConnector.class))
            );
        }
        return endpoints;
    }
}
