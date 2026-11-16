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
package io.gravitee.gateway.handlers.api.secrets;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeListener;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps the secrets of a product version granted while anything still runs on it.
 *
 * <p>Discovery rewrites the secret references of a version in place, so a second discovery finds none: a version must
 * stay bound for as long as a runtime may be built from it. It is revoked when its last binding is released, once the
 * last runtime built from it has stopped and a later version has taken over. Each version gets its own revision, so
 * revoking a replaced version leaves the grants of the next one untouched.</p>
 */
public class ApiProductSecretDiscovery implements ApiProductRuntimeListener {

    private final EventManager eventManager;
    private final Map<ReactableApiProduct, Binding> bindings = new IdentityHashMap<>();
    private final AtomicLong revisions = new AtomicLong();

    public ApiProductSecretDiscovery(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public void beforeStart(ReactableApiProduct product) {
        bind(product);
    }

    @Override
    public void afterStop(ReactableApiProduct product) {
        release(product);
    }

    public synchronized void bind(ReactableApiProduct product) {
        if (product == null) {
            return;
        }
        Binding binding = bindings.get(product);
        if (binding == null) {
            binding = new Binding(String.valueOf(revisions.incrementAndGet()));
            discover(product, binding.revision);
            bindings.put(product, binding);
        }
        binding.count++;
    }

    /** A discovery can fail after granting some of the references, so what it granted is revoked before failing. */
    private void discover(ReactableApiProduct product, String revision) {
        try {
            publish(SecretDiscoveryEventType.DISCOVER, product, revision);
        } catch (RuntimeException discoveryFailure) {
            try {
                publish(SecretDiscoveryEventType.REVOKE, product, revision);
            } catch (RuntimeException revokeFailure) {
                discoveryFailure.addSuppressed(revokeFailure);
            }
            throw discoveryFailure;
        }
    }

    public synchronized void release(ReactableApiProduct product) {
        Binding binding = product == null ? null : bindings.get(product);
        if (binding == null) {
            return;
        }
        if (--binding.count == 0) {
            bindings.remove(product);
            publish(SecretDiscoveryEventType.REVOKE, product, binding.revision);
        }
    }

    private void publish(SecretDiscoveryEventType type, ReactableApiProduct product, String revision) {
        eventManager.publishEvent(type, new SecretDiscoveryEvent(product.getEnvironmentId(), product, new DefinitionMetadata(revision)));
    }

    private static final class Binding {

        private final String revision;
        private int count;

        private Binding(String revision) {
            this.revision = revision;
        }
    }
}
