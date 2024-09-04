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
import io.gravitee.gateway.security.core.exception.MalformedCertificateException;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

@Slf4j
public class SubscriptionTrustStoreLoader extends AbstractKeyStoreLoader<SubscriptionTrustStoreLoader.SubscriptionTrustStoreLoaderOption> {

    private final String id;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final KeyStore keystore;
    private String digest;

    public SubscriptionTrustStoreLoader(Subscription subscription) throws MalformedCertificateException {
        // No particular options required for SubscriptionTrustStoreLoader
        super(SubscriptionTrustStoreLoaderOption.empty());
        this.id = "subscription_cert_%s".formatted(subscription.getId());
        final String pem = new String(Base64.getDecoder().decode(subscription.getClientCertificate()));
        keystore = KeyStoreUtils.initFromPemCertificate(pem, getPassword(), subscription.getId());

        try {
            digest = DigestUtils.md5DigestAsHex(keystore.getCertificate(subscription.getId()).getEncoded());
        } catch (CertificateEncodingException | KeyStoreException e) {
            throw new MalformedCertificateException(
                "An error occurred while computing certificate digest for Subscription %s".formatted(subscription.getId()),
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

    public String certificateDigest() {
        return digest;
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
