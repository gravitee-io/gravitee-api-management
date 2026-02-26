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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.security.CertificateUtils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionTrustStoreLoaderTest {

    static Certificate certificate;
    static String fingerprint;
    private static final String CERTIFICATE = """
        -----BEGIN CERTIFICATE-----
        MIIF3zCCA8egAwIBAgIBZTANBgkqhkiG9w0BAQsFADCBkDEpMCcGCSqGSIb3DQEJ
        ARYaY29udGFjdEBncmF2aXRlZXNvdXJjZS5jb20xEDAOBgNVBAMMB0FQSU1fQ04x
        DTALBgNVBAsMBEFQSU0xFDASBgNVBAoMC0FQSU1fVGVzdGVyMQ4wDAYDVQQHDAVM
        aWxsZTEPMA0GA1UECAwGRnJhbmNlMQswCQYDVQQGEwJGUjAeFw0yNDA4MjgwNjU0
        NDVaFw0yNTA4MjgwNjU0NDVaMIGQMSkwJwYJKoZIhvcNAQkBFhpjb250YWN0QGdy
        YXZpdGVlc291cmNlLmNvbTEQMA4GA1UEAwwHQVBJTV9DTjENMAsGA1UECwwEQVBJ
        TTEUMBIGA1UECgwLQVBJTV9UZXN0ZXIxDjAMBgNVBAcMBUxpbGxlMQ8wDQYDVQQI
        DAZGcmFuY2UxCzAJBgNVBAYTAkZSMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC
        CgKCAgEA+gqld2y/FXbPtCcZgDIzpilAZIsqHpBpDFAx+abDMQ5eWZ3Q3AaxRxhy
        QBxMfhXl2FodBf76mAT8QZoTu0twSJr+LlvyT7515FvbaLX4h6lFRmk6uq1xNCtx
        SAs0/wvKm5W5BkGxrECrXqEuukOJL6unU+tjEqAuKnDXkCd1Up4ZUVjRIWDgGIXW
        3Aubk5Kx0OuFSZXMZi/8aCq0Isqrfy9jeKZ9ef4d2BuhCcqQPAz6t1iM5VTkoPcf
        uNC2XBUB8B8aCiwQQZm2MUlXZ4ha5Wlgc6souFUQr3i37heiXbARTolgVhO1OhCp
        7Um00DlGmBalisd88Bv580ZelSF6WRji/odiUL+rvr8INcVy05PoN+oYcrD5Mfmu
        TeGTPWE6jH1uWoYed02WKz/5xT/Wszl0FvAW90LKRIaZvoyjouWgXa1HQE3XkvyH
        VWyw8xFGLB17OZDk8/4L7cjx6LU1+fzojNzZParrzQ4RhSEXMJaGYB+Y8EnaHCYJ
        luAyUV2CU9WisKoi6ZYoHGyoRrUkrWOu40Yd6BsRheAb7jsjjUzF74Qd7QV5zyMj
        Ydqx3WmO5Jw1reshz/KcTFa/R3ea+POM6wlKGY5MtYiY8Imihvk5UPOjbrGdiSpo
        fhJx9DsVbf7y248QQ8fYwNvLYT1UKVdPittxHJNixHHXKDHXGasCAwEAAaNCMEAw
        HQYDVR0OBBYEFAwJ3VZPsaylIC+RLTyxjigzMk1kMB8GA1UdIwQYMBaAFDcgiEM3
        7YWKFOVsOMMg91eMgTMYMA0GCSqGSIb3DQEBCwUAA4ICAQBCKs4RRIOfhaMkyBgA
        ifRfJuM84UWHRjCFsI5EDIjcpIiQ5/5FroB64OP0g5HE474PKjMWRC5Ld8SP5Vcm
        +YzVYTT2I8a2p5olmbZxOxIYSD45az48WEKKEsqGOuML63oBqEXyhB7hBB6SiJap
        n9qKY6MYptDSDGxutlkd/xbu5TlWK0LLHMe0B+7NGe5P7Z7P/nZkiDbVX7Tm91PH
        /OP80PinU2IdrfQeBP6JuZXznWSwUwsJZRk43S5PAbI+Ao53IBmTxhhonCRXQvXk
        mmdrk++gh85DY1Tk7UClFlK246oLScxAzhz1YlnfKqFmB+jKeUQusl7+YsMU1TAJ
        1jLF5KMSlAhtWto+13tppVlPZbQXhfSWB7lFAXZCNC2r9s5k4KTgAMNCq5TJTPSr
        x/Ft7Q2RWqyFvwXq+Guq8JKb42QzINASVhlBBsaX/6f2SXz9fckatqmzeXt0zp1J
        YaQRlRQ26eh2QgrF3vz5NiQMWzfG9eygYclDPMk2XjX6UJHVcfT27+0IacnuC8ea
        yF9tl+cFs2FjITCUhYSY2Qv3cT1QRp4CrMQ5X0wepQFVzuBframq1VJvDCzan2Sf
        Vi1Yu2gj8bGTf3jhs+y9Enradqo6RXxuOcs13GUeX0UGdn5hTD8M7hKMS3vWHNR9
        cKhxaqcpye8zKJ8+IDsJC2+lMQ==
        -----END CERTIFICATE-----
        """;

    @SneakyThrows
    @BeforeAll
    static void setUp() {
        certificate = KeyStoreUtils.loadPemCertificates(CERTIFICATE)[0];
        fingerprint = CertificateUtils.generateThumbprint((X509Certificate) certificate, "SHA-256");
    }

    @SneakyThrows
    @Test
    void should_build_id_based_on_subscription_id() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(
            new SubscriptionCertificate(Subscription.builder().id("subscriptionId").build(), certificate, fingerprint)
        );
        assertThat(loader.id()).isEqualTo("sub_subscriptionId_cert_" + fingerprint);
    }

    @SneakyThrows
    @Test
    void should_start_a_loader() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(
            new SubscriptionCertificate(Subscription.builder().id("subscriptionId").build(), certificate, fingerprint)
        );

        final List<KeyStoreEvent> keyStoreEvents = new ArrayList<>();
        loader.setEventHandler(keyStoreEvents::add);

        loader.start();
        assertThat(keyStoreEvents)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.loaderId()).isEqualTo(loader.id());
                assertThat(event).isInstanceOf(KeyStoreEvent.LoadEvent.class);
                assertThat(((KeyStoreEvent.LoadEvent) event).keyStore()).satisfies(keyStore -> {
                    assertThat(Collections.list(keyStore.aliases())).containsExactly("cert");
                    assertThat(keyStore.getCertificate("cert")).isEqualTo(certificate);
                });
            });
    }

    @SneakyThrows
    @Test
    void should_not_start_a_loader_already_started() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(
            new SubscriptionCertificate(Subscription.builder().id("subscriptionId").build(), certificate, fingerprint)
        );
        final List<KeyStoreEvent> keyStoreEvents = new ArrayList<>();
        loader.setEventHandler(keyStoreEvents::add);

        loader.start();
        assertThat(keyStoreEvents)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.loaderId()).isEqualTo(loader.id());
                assertThat(event).isInstanceOf(KeyStoreEvent.LoadEvent.class);
            });

        // Try to start again
        loader.start();
        assertThat(keyStoreEvents).hasSize(1);
    }
}
