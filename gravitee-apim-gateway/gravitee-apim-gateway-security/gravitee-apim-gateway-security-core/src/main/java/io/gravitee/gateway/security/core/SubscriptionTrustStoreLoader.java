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

import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.security.core.exception.MalformedCertificateException;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@CustomLog
public class SubscriptionTrustStoreLoader extends AbstractKeyStoreLoader<SubscriptionTrustStoreLoader.SubscriptionTrustStoreLoaderOption> {

    private final String id;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final KeyStore keystore;

    public SubscriptionTrustStoreLoader(SubscriptionCertificate subscriptionCertificate) throws MalformedCertificateException {
        super(SubscriptionTrustStoreLoaderOption.empty());
        try {
            this.id = "sub_%s_cert_%s".formatted(subscriptionCertificate.subscription().getId(), subscriptionCertificate.fingerprint());
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(null, getPassword().toCharArray());
            keystore.setCertificateEntry("cert", subscriptionCertificate.certificate());
        } catch (Exception e) {
            throw new MalformedCertificateException("An error occurred while processing certificate for loader %s".formatted(this.id()), e);
        }
    }

    public static Set<SubscriptionCertificate> readSubscriptionCertificate(Subscription subscription) throws MalformedCertificateException {
        try {
            final byte[] decodedData = Base64.getDecoder().decode(subscription.getClientCertificate());
            var keystore = PKCS7Utils.pkcs7ToTruststore(decodedData, null, i -> "pkcs7-" + i, false).orElseGet(() ->
                KeyStoreUtils.initFromPemCertificate(new String(decodedData), null, "solo")
            );

            Set<SubscriptionCertificate> subscriptionCertificates = new HashSet<>();
            // compute digests from all certificates to be able to find them later
            for (String alias : Collections.list(keystore.aliases())) {
                Certificate certificate = keystore.getCertificate(alias);
                subscriptionCertificates.add(new SubscriptionCertificate(subscription, certificate));
            }
            return subscriptionCertificates;
        } catch (Exception e) {
            throw new MalformedCertificateException(
                "An error occurred while processing certificate for Subscription %s".formatted(subscription.getId()),
                e
            );
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void start() {
        if (!started.get()) {
            started.set(true);
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
