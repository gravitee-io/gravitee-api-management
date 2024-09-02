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
package io.gravitee.gateway.security.core;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.security.KeyStore;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

public class SubscriptionTrustStoreLoader extends AbstractKeyStoreLoader<SubscriptionTrustStoreLoader.SubscriptionTrustStoreLoaderOption> {

    private final Subscription subscription;
    private final String id;
    private AtomicBoolean started = new AtomicBoolean(false);

    public SubscriptionTrustStoreLoader(Subscription subscription) {
        // No particular options required for SubscriptionTrustStoreLoader
        super(SubscriptionTrustStoreLoaderOption.empty());
        this.subscription = subscription;
        this.id = "subscription_cert_%s".formatted(subscription.getId());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void start() {
        if (!started.get()) {
            started.set(true);
            final String pem = new String(Base64.getDecoder().decode(subscription.getClientCertificate()));
            final KeyStore keystore = KeyStoreUtils.initFromPemCertificate(pem, getPassword(), subscription.getId());
            onEvent(new KeyStoreEvent.LoadEvent(id(), keystore, getPassword()));
        }
    }

    @Override
    public void stop() {
        // nothing to do
    }

    @Getter
    @SuperBuilder
    @ToString
    public static class SubscriptionTrustStoreLoaderOption extends AbstractStoreLoaderOptions {

        public static SubscriptionTrustStoreLoaderOption empty() {
            return SubscriptionTrustStoreLoaderOption.builder().build();
        }
    }
}
