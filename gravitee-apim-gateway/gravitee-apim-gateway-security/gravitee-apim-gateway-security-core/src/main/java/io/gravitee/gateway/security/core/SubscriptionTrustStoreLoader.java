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

import io.gravitee.common.security.CertificateUtils;
import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.security.core.exception.MalformedCertificateException;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.util.DigestUtils;

@CustomLog
public class SubscriptionTrustStoreLoader extends AbstractKeyStoreLoader<SubscriptionTrustStoreLoader.SubscriptionTrustStoreLoaderOption> {

    private final String id;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final KeyStore keystore;
    private final List<String> digests;

    public SubscriptionTrustStoreLoader(Subscription subscription) throws MalformedCertificateException {
        super(SubscriptionTrustStoreLoaderOption.empty());
        this.id = "subscription_cert_%s".formatted(subscription.getId());
        digests = new ArrayList<>();
        try {
            final byte[] decodedData = Base64.getDecoder().decode(subscription.getClientCertificate());
            List<String> aliases = new ArrayList<>();
            keystore = PKCS7Utils.pkcs7ToTruststore(
                decodedData,
                getPassword(),
                i -> {
                    String alias = "%s_%d".formatted(subscription.getId(), i);
                    aliases.add(alias);
                    return alias;
                },
                false
            ).orElseGet(() -> {
                aliases.add(subscription.getId());
                return KeyStoreUtils.initFromPemCertificate(new String(decodedData), getPassword(), subscription.getId());
            });

            // compute digests from all certificates to be able to find them later
            for (String alias : aliases) {
                digests.add(CertificateUtils.generateThumbprint((X509Certificate) keystore.getCertificate(alias), "SHA-256"));
            }
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

    public List<String> certificateDigests() {
        return digests;
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
