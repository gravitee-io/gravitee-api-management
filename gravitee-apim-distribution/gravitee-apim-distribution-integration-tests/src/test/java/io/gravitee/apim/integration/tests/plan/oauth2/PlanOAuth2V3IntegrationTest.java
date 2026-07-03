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
package io.gravitee.apim.integration.tests.plan.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.definition.model.ExecutionMode.V3;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.mockito.stubbing.OngoingStubbing;

/**
 * @author GraviteeSource Team
 */
@GatewayTest(v2ExecutionMode = V3)
public class PlanOAuth2V3IntegrationTest extends PlanOAuth2V4EmulationIntegrationTest {

    /**
     * This overrides subscription search :
     * - in jupiter its searched with getByApiAndSecurityToken
     * - in V3 its searches with api/clientId/plan
     */
    @Override
    protected OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
        return when(getBean(SubscriptionService.class).getByApiAndClientIdAndPlan(api, clientId, plan));
    }

    @Override
    protected void should_return_401_unauthorized_with_wrong_security(
        final String apiId,
        final String headerName,
        final String headerValue,
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        client
            .rxRequest(GET, getApiPath(apiId))
            .flatMap(request -> {
                if (headerName != null && headerValue != null) {
                    request.putHeader(headerName, headerValue);
                }
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(401);
                return response.rxBody();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("Unauthorized");
                return true;
            });

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
