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
package io.gravitee.apim.integration.tests.plan.mtls;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.integration.tests.plan.PlanHelper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * APIM-14863: {@code http.ssl.sendClientCertificateAuthorities} lets a deployment advertise the configured trust
 * store again, for clients that need the list to pick which certificate to present. It must never widen back to the
 * certificates registered at runtime by mTLS plan subscriptions — that is the disclosure the ticket reported.
 *
 * @see PlanMutualTLSCertificateAuthoritiesNotAdvertisedIntegrationTest
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
class PlanMutualTLSCertificateAuthoritiesAdvertisedIntegrationTest extends AbstractPlanMutualTLSCertificateAuthoritiesIntegrationTest {

    @Override
    protected Boolean sendClientCertificateAuthorities() {
        return true;
    }

    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @ParameterizedTest
    @ValueSource(strings = { "TLSv1.2", "TLSv1.3" })
    void should_advertise_only_the_configured_trust_store(String protocol, GatewayDynamicConfig.Config gatewayConfig) throws Exception {
        Subscription subscription = registerSubscription();
        try {
            List<String> advertised = advertisedCertificateAuthorities(gatewayConfig.httpPort(), protocol);

            assertThat(advertised).containsExactly(subjectDn(CONFIGURED_TRUST_STORE));
            assertThat(advertised).doesNotContain(subjectDn(SUBSCRIPTION_LEAF));
        } finally {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
        }
    }

    /**
     * The consequence the distribution {@code gravitee.yml}, the Helm {@code values.yaml} and the chart CHANGELOG all
     * warn about, asserted rather than only written down: a non-empty advertised list is a constraint, not a hint.
     * Subscription certificates are self-signed leaves, so they are never issued by an authority of the configured
     * trust store, and a JDK client withholds a certificate whose issuer is absent from the list.
     *
     * <p>The same call returns 200 in the default configuration
     * ({@link PlanMutualTLSCertificateAuthoritiesNotAdvertisedIntegrationTest}). The day this goes green, the warning
     * in both configuration files has become wrong.
     */
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @Test
    void should_cut_off_a_subscription_whose_certificate_is_not_issued_by_an_advertised_authority(@WithCert HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        Subscription subscription = registerSubscription();
        try {
            client
                .rxRequest(GET, PlanHelper.getApiPath("v4-proxy-api"))
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED_401);
                    return response.rxBody();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body.toString()).contains(MtlsPolicy.FAILURE_MESSAGE);
                    return true;
                });
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        } finally {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
        }
    }
}
