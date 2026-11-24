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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuardrailStepsUpgraderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    private GuardrailStepsUpgrader upgrader;

    @BeforeEach
    void setUp() throws Exception {
        upgrader = new GuardrailStepsUpgrader(apiRepository, environmentRepository, objectMapper);
        var environment = new Environment();
        environment.setId("env-1");
        when(environmentRepository.findAll()).thenReturn(Set.of(environment));
    }

    @Test
    void should_run_the_guardrails_of_the_internal_flow_through_the_guardrail_policy() throws Exception {
        givenApis(
            anApi(
                """
                {"type":"llm-proxy","internalFlow":{"enabled":true,
                  "request":[{"name":"PII","policy":"guardian-policy","configuration":{"guardianId":"guardian-1"}},
                             {"name":"Quota","policy":"rate-limit","configuration":{"guardianId":"not-a-guardian"}}],
                  "response":[{"name":"Toxicity","policy":"guardian-policy","configuration":{"guardianId":"guardian-2","onError":"BLOCK"}}]}}
                """
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        JsonNode flow = updatedDefinition().at("/internalFlow");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(flow.at("/request/0/policy").asText()).as("request guardrail policy").isEqualTo("guardrail-policy");
            softly.assertThat(flow.at("/request/0/configuration/guardrailId").asText()).as("request guardrail").isEqualTo("guardian-1");
            softly.assertThat(flow.at("/request/0/configuration/guardianId").isMissingNode()).as("request guardian key left").isTrue();
            softly.assertThat(flow.at("/request/1/policy").asText()).as("other policy").isEqualTo("rate-limit");
            softly
                .assertThat(flow.at("/request/1/configuration/guardianId").asText())
                .as("other policy configuration")
                .isEqualTo("not-a-guardian");
            softly.assertThat(flow.at("/response/0/policy").asText()).as("response guardrail policy").isEqualTo("guardrail-policy");
            softly.assertThat(flow.at("/response/0/configuration/guardrailId").asText()).as("response guardrail").isEqualTo("guardian-2");
            softly.assertThat(flow.at("/response/0/configuration/onError").asText()).as("other configuration key").isEqualTo("BLOCK");
            softly.assertThat(flow.at("/response/0/name").asText()).as("step name").isEqualTo("Toxicity");
        });
    }

    @Test
    void should_leave_an_api_without_guardian_step_untouched() throws Exception {
        givenApis(
            anApi(
                """
                {"type":"llm-proxy","internalFlow":{"request":[{"policy":"rate-limit","configuration":{}}]}}
                """
            ),
            anApi(
                """
                {"type":"proxy","flows":[]}
                """
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(apiRepository, never()).update(any());
    }

    @Test
    void should_leave_an_api_already_running_the_guardrail_policy_untouched() throws Exception {
        givenApis(
            anApi(
                """
                {"type":"llm-proxy","internalFlow":{"request":[{"policy":"guardrail-policy","configuration":{"guardrailId":"g-1"}}]}}
                """
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(apiRepository, never()).update(any());
    }

    private void givenApis(Api... apis) throws Exception {
        when(apiRepository.search(any(), any(), any())).thenReturn(Stream.of(apis));
    }

    private static Api anApi(String definition) {
        var api = new Api();
        api.setId("api-" + definition.hashCode());
        api.setEnvironmentId("env-1");
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setDefinition(definition);
        return api;
    }

    private JsonNode updatedDefinition() throws Exception {
        var captor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository).update(captor.capture());
        return objectMapper.readTree(captor.getValue().getDefinition());
    }
}
