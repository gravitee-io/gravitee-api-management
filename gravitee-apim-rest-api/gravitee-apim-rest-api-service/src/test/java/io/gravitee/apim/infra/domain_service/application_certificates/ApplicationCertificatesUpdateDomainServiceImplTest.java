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
package io.gravitee.apim.infra.domain_service.application_certificates;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ClientCertificateCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApplicationCertificatesUpdateDomainServiceImplTest {

    private static final String APPLICATION_ID = "app-id";
    private static final String PLAN_ID = "plan-id";
    private static final String API_ID = "api-id";
    private static final String SUBSCRIPTION_ID = "sub-id";
    private static final String ENVIRONMENT_ID = "env-id";

    private static final String PEM_CERTIFICATE_1 = """
        -----BEGIN CERTIFICATE-----
        MIIDCTCCAfGgAwIBAgIUdh1NFpteTomLlWoO7O5HKI7fg10wDQYJKoZIhvcNAQEL
        BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDEyOTE0MTYyMVoXDTI3MDEy
        OTE0MTYyMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
        AAOCAQ8AMIIBCgKCAQEAwquFLM7wi+EBt5JL1Q6c/qzqickBKhlymOf1a18jYsWO
        UrRRqANMvEub5zks44qdYbdm7Kj9EruDD3hfrS6YemQhIMeT+SgY3MU5cY12yB1e
        fn+0bkEg6CJdIfgvcuccqY9pu0hgFdlgK9YXEXYZzb+ai7b4qPHN2BR6toBdjf56
        ReNygcrT0igPQC9P/MsmpFuJzD69i5Z8fJcLU2V7RwXW9MKyej8CN/zrcqftG+ck
        egt5cpB2OyIt6ajQBeXarGYvOlm975RKWOD5mHpt87GgcEvMEnhFTSs47iA7X91o
        01+rhFDfPxgvhur2AG6oiB7/zS4lqHWQHRTBwurVJQIDAQABo1MwUTAdBgNVHQ4E
        FgQUHP3c1CNhl6RZHn3g/HkbSssfPhMwHwYDVR0jBBgwFoAUHP3c1CNhl6RZHn3g
        /HkbSssfPhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAgTa/
        lgY6fEbt88yQcLxQDseam2lox0sK6sYOwpIQV7/RiXbeM6KrXmCy59HBrJMjSpZY
        LEp9RPDf8Awg50iv6oFXXn+ZJ5Cmq2WXMpCvKxAjQWnmNs99SGfXyQsiLMxe3HlL
        CKqM8O7LZrVdOxWbNW/0ZMJl4d4vCf0LhVrbfMGLeQfqtKVmygjJM1rycKiFazM4
        cTHphvWA9/5XRFC+yD3V3ZTFE9LDeMoSF0soigR0NnCqFc5E7S9OQvuB5h5eBu9s
        T1dLBVOY8zcIYu6LDjeMr6MpiZyk+/O7ewue2kQURmDBuVrwiSSQF4AhsmbMDUuB
        iyr26LfMYtituDZy7w==
        -----END CERTIFICATE-----
        """;

    private static final String PEM_CERTIFICATE_2 = """
        -----BEGIN CERTIFICATE-----
        MIIDCTCCAfGgAwIBAgIUYh2NFpteTomLlWoO7O5HKI7fg10wDQYJKoZIhvcNAQEL
        BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDEyOTE0MTYyMVoXDTI3MDEy
        OTE0MTYyMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
        AAOCAQ8AMIIBCgKCAQEAwquFLM7wi+EBt5JL1Q6c/qzqickBKhlymOf1a18jYsWO
        UrRRqANMvEub5zks44qdYbdm7Kj9EruDD3hfrS6YemQhIMeT+SgY3MU5cY12yB1e
        fn+0bkEg6CJdIfgvcuccqY9pu0hgFdlgK9YXEXYZzb+ai7b4qPHN2BR6toBdjf56
        ReNygcrT0igPQC9P/MsmpFuJzD69i5Z8fJcLU2V7RwXW9MKyej8CN/zrcqftG+ck
        egt5cpB2OyIt6ajQBeXarGYvOlm975RKWOD5mHpt87GgcEvMEnhFTSs47iA7X91o
        01+rhFDfPxgvhur2AG6oiB7/zS4lqHWQHRTBwurVJQIDAQABo1MwUTAdBgNVHQ4E
        FgQUHP3c1CNhl6RZHn3g/HkbSssfPhMwHwYDVR0jBBgwFoAUHP3c1CNhl6RZHn3g
        /HkbSssfPhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAgTa/
        lgY6fEbt88yQcLxQDseam2lox0sK6sYOwpIQV7/RiXbeM6KrXmCy59HBrJMjSpZY
        LEp9RPDf8Awg50iv6oFXXn+ZJ5Cmq2WXMpCvKxAjQWnmNs99SGfXyQsiLMxe3HlL
        CKqM8O7LZrVdOxWbNW/0ZMJl4d4vCf0LhVrbfMGLeQfqtKVmygjJM1rycKiFazM4
        cTHphvWA9/5XRFC+yD3V3ZTFE9LDeMoSF0soigR0NnCqFc5E7S9OQvuB5h5eBu9s
        T1dLBVOY8zcIYu6LDjeMr6MpiZyk+/O7ewue2kQURmDBuVrwiSSQF4AhsmbMDUuB
        iyr26LfMYtituDZy7w==
        -----END CERTIFICATE-----
        """;

    private ClientCertificateCrudServiceInMemory clientCertificateCrudService;
    private SubscriptionCrudServiceInMemory subscriptionCrudService;
    private SubscriptionQueryServiceInMemory subscriptionQueryService;
    private PlanCrudServiceInMemory planCrudService;
    private ApplicationCertificatesUpdateDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        clientCertificateCrudService = new ClientCertificateCrudServiceInMemory();
        subscriptionCrudService = new SubscriptionCrudServiceInMemory();
        planCrudService = new PlanCrudServiceInMemory();
        subscriptionQueryService = new SubscriptionQueryServiceInMemory(subscriptionCrudService, planCrudService);

        service = new ApplicationCertificatesUpdateDomainServiceImpl(
            subscriptionQueryService,
            subscriptionCrudService,
            clientCertificateCrudService
        );
    }

    @AfterEach
    void tearDown() {
        clientCertificateCrudService.reset();
        subscriptionCrudService.reset();
        planCrudService.reset();
    }

    @Test
    void should_not_update_ActiveMTLSSubscriptions_when_no_mtls_subscriptions() {
        // Given: an API key plan (not mTLS)
        Plan apiKeyPlan = buildPlan(PLAN_ID, PlanSecurityType.API_KEY.getLabel());
        planCrudService.initWith(List.of(apiKeyPlan));

        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription));

        ClientCertificate certificate = buildClientCertificate("cert-1", APPLICATION_ID, ClientCertificateStatus.ACTIVE, PEM_CERTIFICATE_1);
        clientCertificateCrudService.initWith(List.of(certificate));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should not be updated
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isNull();
    }

    @Test
    void should_update_ActiveMTLSSubscriptions_subscription_with_single_certificate_as_base64_pem() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription));

        ClientCertificate certificate = buildClientCertificate("cert-1", APPLICATION_ID, ClientCertificateStatus.ACTIVE, PEM_CERTIFICATE_1);
        clientCertificateCrudService.initWith(List.of(certificate));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should be updated with base64 encoded PEM
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isNotNull();

        String decodedCertificate = new String(Base64.getDecoder().decode(result.getClientCertificate()), StandardCharsets.UTF_8);
        assertThat(decodedCertificate).isEqualTo(PEM_CERTIFICATE_1);
    }

    @Test
    void should_update_ActiveMTLSSubscriptions_subscription_with_pkcs7_bundle_when_multiple_certificates() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription));

        ClientCertificate certificate1 = buildClientCertificate(
            "cert-1",
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE,
            PEM_CERTIFICATE_1
        );
        ClientCertificate certificate2 = buildClientCertificate(
            "cert-2",
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE_WITH_END,
            PEM_CERTIFICATE_2
        );
        clientCertificateCrudService.initWith(List.of(certificate1, certificate2));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should be updated with base64 encoded PKCS7 bundle
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isNotNull();

        // Verify it's a valid base64 string and starts with PKCS7 signature
        byte[] decoded = Base64.getDecoder().decode(result.getClientCertificate());
        assertThat(decoded).isNotEmpty();
        // PKCS7 DER format starts with 0x30 (SEQUENCE tag)
        assertThat(decoded[0]).isEqualTo((byte) 0x30);
    }

    @Test
    void should_not_update_ActiveMTLSSubscriptions_subscription_when_certificate_unchanged() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        String existingEncodedCert = Base64.getEncoder().encodeToString(PEM_CERTIFICATE_1.getBytes(StandardCharsets.UTF_8));
        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, existingEncodedCert);
        subscriptionCrudService.initWith(List.of(subscription));

        ClientCertificate certificate = buildClientCertificate("cert-1", APPLICATION_ID, ClientCertificateStatus.ACTIVE, PEM_CERTIFICATE_1);
        clientCertificateCrudService.initWith(List.of(certificate));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should remain unchanged (same reference)
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isEqualTo(existingEncodedCert);
    }

    @Test
    void should_clear_certificate_when_no_active_certificates() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        String existingEncodedCert = Base64.getEncoder().encodeToString(PEM_CERTIFICATE_1.getBytes(StandardCharsets.UTF_8));
        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, existingEncodedCert);
        subscriptionCrudService.initWith(List.of(subscription));

        // No active certificates (only revoked)
        ClientCertificate revokedCertificate = buildClientCertificate(
            "cert-1",
            APPLICATION_ID,
            ClientCertificateStatus.REVOKED,
            PEM_CERTIFICATE_1
        );
        clientCertificateCrudService.initWith(List.of(revokedCertificate));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should have null certificate
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isNull();
    }

    @Test
    void should_update_ActiveMTLSSubscriptions_multiple_subscriptions() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        SubscriptionEntity subscription1 = buildSubscription("sub-1", APPLICATION_ID, PLAN_ID, null);
        SubscriptionEntity subscription2 = buildSubscription("sub-2", APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription1, subscription2));

        ClientCertificate certificate = buildClientCertificate("cert-1", APPLICATION_ID, ClientCertificateStatus.ACTIVE, PEM_CERTIFICATE_1);
        clientCertificateCrudService.initWith(List.of(certificate));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: both subscriptions should be updated
        String expectedEncodedCert = Base64.getEncoder().encodeToString(PEM_CERTIFICATE_1.getBytes(StandardCharsets.UTF_8));
        assertThat(subscriptionCrudService.get("sub-1").getClientCertificate()).isEqualTo(expectedEncodedCert);
        assertThat(subscriptionCrudService.get("sub-2").getClientCertificate()).isEqualTo(expectedEncodedCert);
    }

    @Test
    void should_order_certificates_by_created_at_when_creating_pkcs7_bundle() throws Exception {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription));

        // Create certificates with different creation dates - cert2 is older than cert1
        Date olderDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Date newerDate = Date.from(Instant.now());

        ClientCertificate certificate1 = buildClientCertificate(
            "cert-1",
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE,
            PEM_CERTIFICATE_1,
            olderDate
        );
        ClientCertificate certificate2 = buildClientCertificate(
            "cert-2",
            APPLICATION_ID,
            ClientCertificateStatus.ACTIVE,
            PEM_CERTIFICATE_2,
            newerDate
        );
        // Add in reverse order to ensure sorting is happening
        clientCertificateCrudService.initWith(List.of(certificate1, certificate2));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: subscription should be updated with base64 encoded PKCS7 bundle
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        assertThat(result.getClientCertificate()).isNotNull();

        byte[] pkcs7 = Base64.getDecoder().decode(result.getClientCertificate());
        Optional<KeyStore> keyStore = PKCS7Utils.pkcs7ToTruststore(pkcs7, null, String::valueOf, false);
        assertThat(keyStore).isPresent();
        KeyStore ks = keyStore.get();
        assertThat(ks.getCertificate("0")).isEqualTo(KeyStoreUtils.loadPemCertificates(PEM_CERTIFICATE_1)[0]);
        assertThat(ks.getCertificate("1")).isEqualTo(KeyStoreUtils.loadPemCertificates(PEM_CERTIFICATE_2)[0]);
    }

    @Test
    void should_only_consider_active_and_active_with_end_certificates() {
        // Given: an mTLS plan
        Plan mtlsPlan = buildPlan(PLAN_ID, PlanSecurityType.MTLS.getLabel());
        planCrudService.initWith(List.of(mtlsPlan));

        SubscriptionEntity subscription = buildSubscription(SUBSCRIPTION_ID, APPLICATION_ID, PLAN_ID, null);
        subscriptionCrudService.initWith(List.of(subscription));

        // Mix of certificate statuses
        ClientCertificate activeCert = buildClientCertificate("cert-1", APPLICATION_ID, ClientCertificateStatus.ACTIVE, PEM_CERTIFICATE_1);
        ClientCertificate scheduledCert = buildClientCertificate(
            "cert-2",
            APPLICATION_ID,
            ClientCertificateStatus.SCHEDULED,
            PEM_CERTIFICATE_2
        );
        ClientCertificate revokedCert = buildClientCertificate(
            "cert-3",
            APPLICATION_ID,
            ClientCertificateStatus.REVOKED,
            PEM_CERTIFICATE_2
        );
        clientCertificateCrudService.initWith(List.of(activeCert, scheduledCert, revokedCert));

        // When
        service.updateActiveMTLSSubscriptions(APPLICATION_ID);

        // Then: only the ACTIVE certificate should be used (single cert = base64 PEM)
        SubscriptionEntity result = subscriptionCrudService.get(SUBSCRIPTION_ID);
        String decodedCertificate = new String(Base64.getDecoder().decode(result.getClientCertificate()), StandardCharsets.UTF_8);
        assertThat(decodedCertificate).isEqualTo(PEM_CERTIFICATE_1);
    }

    private Plan buildPlan(String planId, String securityType) {
        io.gravitee.definition.model.v4.plan.Plan planDefinition = io.gravitee.definition.model.v4.plan.Plan.builder()
            .id(planId)
            .security(PlanSecurity.builder().type(securityType).build())
            .build();

        return Plan.builder().id(planId).apiId(API_ID).definitionVersion(DefinitionVersion.V4).planDefinitionHttpV4(planDefinition).build();
    }

    private SubscriptionEntity buildSubscription(String subscriptionId, String applicationId, String planId, String clientCertificate) {
        return SubscriptionEntity.builder()
            .id(subscriptionId)
            .applicationId(applicationId)
            .planId(planId)
            .apiId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .status(SubscriptionEntity.Status.ACCEPTED)
            .clientCertificate(clientCertificate)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .build();
    }

    private ClientCertificate buildClientCertificate(
        String id,
        String applicationId,
        ClientCertificateStatus status,
        String pemCertificate
    ) {
        return buildClientCertificate(id, applicationId, status, pemCertificate, new Date());
    }

    private ClientCertificate buildClientCertificate(
        String id,
        String applicationId,
        ClientCertificateStatus status,
        String pemCertificate,
        Date createdAt
    ) {
        return new ClientCertificate(
            id,
            "cross-id-" + id,
            applicationId,
            "Test Certificate " + id,
            null,
            null,
            createdAt,
            new Date(),
            pemCertificate,
            Date.from(Instant.now().plus(365, ChronoUnit.DAYS)),
            "CN=localhost",
            "CN=localhost",
            "fingerprint-" + id,
            ENVIRONMENT_ID,
            status
        );
    }
}
