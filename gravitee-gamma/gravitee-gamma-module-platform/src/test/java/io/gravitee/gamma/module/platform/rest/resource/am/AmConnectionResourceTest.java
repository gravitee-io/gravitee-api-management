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
package io.gravitee.gamma.module.platform.rest.resource.am;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.GetAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.SaveAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.TestAmConnectionUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Resource-level wiring test: confirms the JAX-RS resource forwards request-body fields into the
// use-case Input. Guards the regression where /_test silently dropped amOrganizationId. Mockito
// @InjectMocks fills the resource's @Inject fields by type, so no Jersey container is needed.
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmConnectionResourceTest {

    @Mock
    private GetAmConnectionUseCase getAmConnectionUseCase;

    @Mock
    private SaveAmConnectionUseCase saveAmConnectionUseCase;

    @Mock
    private TestAmConnectionUseCase testAmConnectionUseCase;

    @InjectMocks
    private AmConnectionResource resource;

    private AmConnectionRequest request(String amOrganizationId) {
        return new AmConnectionRequest("https://am.example", "token", amOrganizationId, "env", "dom", "dom-hrid", "https://gw");
    }

    @Test
    void test_endpoint_forwards_am_organization_id_from_body() {
        when(testAmConnectionUseCase.execute(any())).thenReturn(new TestAmConnectionUseCase.Output(AmConnectionTestResult.success()));

        resource.test("APIM-ORG", request("am-org"));

        var captor = ArgumentCaptor.forClass(TestAmConnectionUseCase.Input.class);
        verify(testAmConnectionUseCase).execute(captor.capture());
        assertThat(captor.getValue().orgId()).isEqualTo("APIM-ORG");
        assertThat(captor.getValue().inboundBaseUrl()).isEqualTo("https://am.example");
        assertThat(captor.getValue().inboundAmOrganizationId()).isEqualTo("am-org");
    }

    @Test
    void test_endpoint_tolerates_a_null_body() {
        when(testAmConnectionUseCase.execute(any())).thenReturn(new TestAmConnectionUseCase.Output(AmConnectionTestResult.success()));

        resource.test("APIM-ORG", null);

        var captor = ArgumentCaptor.forClass(TestAmConnectionUseCase.Input.class);
        verify(testAmConnectionUseCase).execute(captor.capture());
        assertThat(captor.getValue().orgId()).isEqualTo("APIM-ORG");
        assertThat(captor.getValue().inboundBaseUrl()).isNull();
        assertThat(captor.getValue().inboundAmOrganizationId()).isNull();
    }

    @Test
    void save_endpoint_forwards_am_organization_id_from_body() {
        when(saveAmConnectionUseCase.execute(any())).thenReturn(
            new SaveAmConnectionUseCase.Output("https://am.example", true, "am-org", "env", "dom", "dom-hrid", "https://gw")
        );

        resource.save("APIM-ORG", request("am-org"));

        var captor = ArgumentCaptor.forClass(SaveAmConnectionUseCase.Input.class);
        verify(saveAmConnectionUseCase).execute(captor.capture());
        assertThat(captor.getValue().orgId()).isEqualTo("APIM-ORG");
        assertThat(captor.getValue().amOrganizationId()).isEqualTo("am-org");
    }
}
