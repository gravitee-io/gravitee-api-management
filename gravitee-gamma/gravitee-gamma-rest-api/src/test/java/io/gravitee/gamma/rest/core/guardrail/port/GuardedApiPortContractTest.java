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
package io.gravitee.gamma.rest.core.guardrail.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardrail.model.ApiReference;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every {@link GuardedApiPort} implementation must honour, whatever API store sits behind it.
 *
 * <p>The implementations do not answer {@code findByGuardrail} the same way — the real one reads the index the API
 * store keeps beside the definition, the in-memory one reads the internal flow itself — so the one answer that
 * matters is pinned here: what the port says an API runs, after the port wrote it, is what it runs. Without this,
 * every domain test would stay green on a store that never indexed anything, and the refusal to withdraw a Guardrail
 * an API runs would quietly stop refusing.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class GuardedApiPortContractTest {

    protected static final String ENVIRONMENT_ID = "environment-id";
    protected static final String OTHER_ENVIRONMENT_ID = "other-environment";
    protected static final String ORGANIZATION_ID = "organization-id";
    protected static final String API_ID = "api-1";
    protected static final String API_NAME = "Support assistant";

    /** Every write is scoped by the environment of its caller, so the write takes the caller rather than an id. */
    protected static final AuditInfo CALLER = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();

    protected abstract GuardedApiPort port();

    /** Stores an API able to run Guardrails, running the Guardrails of {@code internalFlow}. */
    protected abstract void givenApi(String environmentId, String apiId, String name, Flow internalFlow);

    protected static Flow running(String guardrailId) {
        return Flow.builder().request(List.of(GuardrailSteps.of(guardrailId, guardrailId))).build();
    }

    @Test
    void should_read_the_internal_flow_of_an_api_of_the_environment() {
        var internalFlow = running("guardrail-1");
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, internalFlow);

        assertThat(port().findById(ENVIRONMENT_ID, API_ID))
            .as("API of the environment")
            .get()
            .satisfies(api ->
                assertSoftly(softly -> {
                    softly.assertThat(api.id()).as("id").isEqualTo(API_ID);
                    softly.assertThat(api.environmentId()).as("environment").isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(api.name()).as("name").isEqualTo(API_NAME);
                    softly.assertThat(api.supportsGuardrails()).as("supports guardrails").isTrue();
                    softly.assertThat(api.internalFlow()).as("internal flow").isEqualTo(internalFlow);
                })
            );
    }

    @Test
    void should_not_read_an_api_of_another_environment() {
        givenApi(OTHER_ENVIRONMENT_ID, API_ID, API_NAME, running("guardrail-1"));

        assertThat(port().findById(ENVIRONMENT_ID, API_ID)).as("API of another environment").isEmpty();
    }

    @Test
    void should_list_the_apis_of_the_environment_running_the_guardrail() {
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, running("guardrail-1"));
        givenApi(ENVIRONMENT_ID, "api-2", "Other", running("guardrail-2"));
        givenApi(OTHER_ENVIRONMENT_ID, "api-3", "Elsewhere", running("guardrail-1"));

        assertThat(port().findByGuardrail(ENVIRONMENT_ID, "guardrail-1", 1, 10).getContent())
            .as("APIs of the environment running guardrail-1")
            .containsExactly(new ApiReference(API_ID, API_NAME));
    }

    @Test
    void should_list_no_api_for_a_guardrail_none_runs() {
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, running("guardrail-1"));

        assertThat(port().findByGuardrail(ENVIRONMENT_ID, "guardrail-2", 1, 10).getContent()).as("APIs running guardrail-2").isEmpty();
    }

    /** Turning a Guardrail off stops the API from holding it back, with no second write to make that true. */
    @Test
    void should_stop_listing_an_api_the_port_itself_turned_the_guardrail_off_on() {
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, running("guardrail-1"));

        port().updateInternalFlow(CALLER, API_ID, running("guardrail-1"), null);

        assertThat(port().findByGuardrail(ENVIRONMENT_ID, "guardrail-1", 1, 10).getContent())
            .as("APIs running guardrail-1 after it was turned off")
            .isEmpty();
    }

    @Test
    void should_count_every_api_running_the_guardrail_whatever_the_page_asked() {
        givenApi(ENVIRONMENT_ID, API_ID, "Assistant one", running("guardrail-1"));
        givenApi(ENVIRONMENT_ID, "api-2", "Assistant two", running("guardrail-1"));

        var firstPage = port().findByGuardrail(ENVIRONMENT_ID, "guardrail-1", 1, 1);

        assertSoftly(softly -> {
            softly.assertThat(firstPage.getContent()).as("first page of one").hasSize(1);
            softly.assertThat(firstPage.getTotalElements()).as("APIs running guardrail-1").isEqualTo(2);
            softly.assertThat(port().findByGuardrail(ENVIRONMENT_ID, "guardrail-1", 2, 1).getContent()).as("second page of one").hasSize(1);
        });
    }

    @Test
    void should_replace_the_internal_flow_of_an_api_of_the_environment() {
        var stored = running("guardrail-1");
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, stored);
        var replacement = running("guardrail-2");

        var updated = port().updateInternalFlow(CALLER, API_ID, stored, replacement);

        assertSoftly(softly -> {
            softly.assertThat(updated.internalFlow()).as("internal flow returned").isEqualTo(replacement);
            softly
                .assertThat(port().findById(ENVIRONMENT_ID, API_ID))
                .as("internal flow read back")
                .get()
                .satisfies(api -> assertThat(api.internalFlow()).isEqualTo(replacement));
        });
    }

    /** The write is what feeds whatever {@code findByGuardrail} reads; nothing else may have to be written by hand. */
    @Test
    void should_list_the_api_running_a_guardrail_the_port_itself_turned_on() {
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, null);

        port().updateInternalFlow(CALLER, API_ID, null, running("guardrail-1"));

        assertThat(port().findByGuardrail(ENVIRONMENT_ID, "guardrail-1", 1, 10).getContent())
            .as("APIs running the Guardrail just turned on")
            .containsExactly(new ApiReference(API_ID, API_NAME));
    }

    @Test
    void should_refuse_to_replace_an_internal_flow_the_api_no_longer_holds() {
        givenApi(ENVIRONMENT_ID, API_ID, API_NAME, running("guardrail-1"));

        assertThatThrownBy(() -> port().updateInternalFlow(CALLER, API_ID, running("guardrail-3"), running("guardrail-2")))
            .as("write from a stale read")
            .isInstanceOf(ApiInternalFlowChangedException.class);
    }

    @Test
    void should_refuse_to_replace_the_internal_flow_of_an_api_of_another_environment() {
        var stored = running("guardrail-1");
        givenApi(OTHER_ENVIRONMENT_ID, API_ID, API_NAME, stored);

        assertThatThrownBy(() -> port().updateInternalFlow(CALLER, API_ID, stored, running("guardrail-2")))
            .as("write on an API of another environment")
            .isInstanceOf(ApiNotFoundException.class);
    }
}
