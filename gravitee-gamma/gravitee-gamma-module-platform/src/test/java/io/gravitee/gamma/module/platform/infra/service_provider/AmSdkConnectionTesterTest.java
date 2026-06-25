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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.am.sdk.management.api.DefaultApi;
import io.gravitee.am.sdk.management.invoker.ApiException;
import io.gravitee.am.sdk.management.model.Environment;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.infra.service_provider.AmSdkClientFactory.AmApis;
import io.vertx.core.Future;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSdkConnectionTesterTest {

    private static final String APIM_ORG = "DEFAULT";

    private AmSdkClientFactory clientFactory;
    private DefaultApi defaults;
    private AmSdkConnectionTester tester;

    @BeforeEach
    void setUp() {
        clientFactory = Mockito.mock(AmSdkClientFactory.class);
        defaults = Mockito.mock(DefaultApi.class);
        tester = new AmSdkConnectionTester(clientFactory);
    }

    private AmConnection connection(String amOrganizationId) {
        return new AmConnection("https://am.example", "token", amOrganizationId, null, null, null, null);
    }

    @Test
    void should_probe_the_configured_am_organization_not_the_apim_org() {
        when(clientFactory.forConnection(any())).thenReturn(new AmApis(null, defaults, null));
        when(defaults.listEnvironments("am-org")).thenReturn(Future.succeededFuture(List.<Environment>of()));

        var result = tester.test(APIM_ORG, connection("am-org"));

        assertThat(result.ok()).isTrue();
        // The whole point of the fix: AM is queried with the connection's amOrganizationId,
        // never the APIM org passed as the first argument.
        verify(defaults).listEnvironments("am-org");
        verify(defaults, never()).listEnvironments(APIM_ORG);
    }

    @Test
    void should_fail_fast_when_am_organization_is_blank_without_calling_am() {
        var result = tester.test(APIM_ORG, connection("   "));

        assertThat(result.ok()).isFalse();
        assertThat(result.status()).isEqualTo(400);
        assertThat(result.message()).contains("AM organization is required");
        verifyNoInteractions(clientFactory);
    }

    @Test
    void should_surface_am_status_and_a_clean_message_when_organization_is_rejected() {
        when(clientFactory.forConnection(any())).thenReturn(new AmApis(null, defaults, null));
        when(defaults.listEnvironments("bogus")).thenReturn(
            Future.failedFuture(new ApiException(403, "Forbidden", null, "{\"message\":\"Permission denied\",\"http_status\":403}"))
        );

        var result = tester.test(APIM_ORG, connection("bogus"));

        assertThat(result.ok()).isFalse();
        // Status still rides along on the result (the UI no longer renders it, but logs/tests can).
        assertThat(result.status()).isEqualTo(403);
        // Readable, framed message — AM's reason unwrapped from the raw JSON body.
        assertThat(result.message()).isEqualTo("Access Management rejected the connection: Permission denied");
        assertThat(result.message()).doesNotContain("{", "http_status");
    }

    @Test
    void should_require_base_url() {
        var result = tester.test(APIM_ORG, new AmConnection("", "token", "am-org", null, null, null, null));

        assertThat(result).isEqualTo(AmConnectionTestResult.failure(400, "baseUrl is required"));
        verifyNoInteractions(clientFactory);
    }

    @Test
    void should_require_access_token() {
        var result = tester.test(APIM_ORG, new AmConnection("https://am.example", " ", "am-org", null, null, null, null));

        assertThat(result.ok()).isFalse();
        assertThat(result.status()).isEqualTo(400);
        assertThat(result.message()).contains("access token is required");
        verifyNoInteractions(clientFactory);
    }
}
