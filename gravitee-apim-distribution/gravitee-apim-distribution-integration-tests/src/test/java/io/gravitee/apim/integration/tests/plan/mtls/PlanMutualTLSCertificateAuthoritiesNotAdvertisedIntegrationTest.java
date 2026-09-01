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
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.integration.tests.plan.PlanHelper;
import io.gravitee.gateway.api.service.Subscription;
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
 * APIM-14863: with the default configuration the gateway must not disclose the client certificates it accepts.
 *
 * <p>Certificates registered at runtime by an mTLS plan subscription are application leaves, not authorities;
 * advertising them told every caller of the listener which client identities are accepted. Since gravitee-node 9.8.0
 * nothing is advertised unless {@code http.ssl.sendClientCertificateAuthorities} is enabled, and runtime
 * certificates are never advertised even then.
 *
 * @see PlanMutualTLSCertificateAuthoritiesAdvertisedIntegrationTest
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
class PlanMutualTLSCertificateAuthoritiesNotAdvertisedIntegrationTest extends AbstractPlanMutualTLSCertificateAuthoritiesIntegrationTest {

    @Override
    protected Boolean sendClientCertificateAuthorities() {
        // left unset on purpose: this pins the default, which is what deployments actually run
        return null;
    }

    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @ParameterizedTest
    @ValueSource(strings = { "TLSv1.2", "TLSv1.3" })
    void should_not_advertise_any_certificate_authority(String protocol, GatewayDynamicConfig.Config gatewayConfig) throws Exception {
        Subscription subscription = registerSubscription();
        try {
            List<String> advertised = advertisedCertificateAuthorities(gatewayConfig.httpPort(), protocol);

            assertThat(advertised).isEmpty();
        } finally {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
        }
    }

    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @Test
    void should_still_accept_the_certificate_registered_by_the_subscription(@WithCert HttpClient client) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        Subscription subscription = registerSubscription();
        try {
            client
                .rxRequest(GET, PlanHelper.getApiPath("v4-proxy-api"))
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(OK_200);
                    return response.rxBody();
                })
                .test()
                .awaitDone(30, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(body -> {
                    assertThat(body.toString()).contains("endpoint response");
                    return true;
                });
        } finally {
            subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
        }
    }
}
