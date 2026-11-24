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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiReadPermissionPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.ApiReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListGuardrailApisUseCaseTest {

    private final InMemoryGuardrailRepository guardrails = new InMemoryGuardrailRepository();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final InMemoryGuardedApiReadPermissionPort permissions = new InMemoryGuardedApiReadPermissionPort();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("reader").build())
        .build();
    private final ListGuardrailApisUseCase useCase = new ListGuardrailApisUseCase(guardrails, apis, permissions);

    @Test
    void should_list_the_apis_of_the_environment_running_the_guardrail_by_name() {
        guardrails.givenGuardrail(aGuardrail().build());
        var runningIt = Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build();
        apis.givenApi(aGuardedApi().id("api-2").name("Support assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("api-1").name("Coding assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("api-3").name("Translator").build());
        apis.givenApi(aGuardedApi().id("api-4").environmentId("other-env").internalFlow(runningIt).build());

        var output = useCase.execute(new ListGuardrailApisUseCase.Input(actor, GUARDRAIL_ID, 0, 0));

        assertThat(output.apis().getContent())
            .as("APIs running the guardrail")
            .containsExactly(new ApiReference("api-1", "Coding assistant"), new ApiReference("api-2", "Support assistant"));
    }

    @Test
    void should_serve_one_page_of_the_apis_running_the_guardrail() {
        guardrails.givenGuardrail(aGuardrail().build());
        var runningIt = Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build();
        apis.givenApi(aGuardedApi().id("api-1").name("Coding assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("api-2").name("Support assistant").internalFlow(runningIt).build());

        var output = useCase.execute(new ListGuardrailApisUseCase.Input(actor, GUARDRAIL_ID, 2, 1));

        assertSoftly(softly -> {
            softly
                .assertThat(output.apis().getContent())
                .as("second page of one")
                .containsExactly(new ApiReference("api-2", "Support assistant"));
            softly.assertThat(output.apis().getTotalElements()).as("APIs running the guardrail").isEqualTo(2);
            softly.assertThat(output.perPage()).as("page size served").isEqualTo(1);
        });
    }

    /**
     * No name of an API the caller may not read leaves this list. The count stays whole, so the caller still learns
     * that something is holding the Guardrail back — it just does not learn whose API it is.
     */
    @Test
    void should_name_only_the_apis_the_caller_may_read() {
        guardrails.givenGuardrail(aGuardrail().build());
        var runningIt = Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build();
        apis.givenApi(aGuardedApi().id("mine").name("Coding assistant").internalFlow(runningIt).build());
        apis.givenApi(aGuardedApi().id("theirs").name("Someone else's assistant").internalFlow(runningIt).build());
        permissions.givenUnreadable("theirs");

        var output = useCase.execute(new ListGuardrailApisUseCase.Input(actor, GUARDRAIL_ID, 0, 0));

        assertSoftly(softly -> {
            softly
                .assertThat(output.apis().getContent())
                .as("APIs named to the caller")
                .containsExactly(new ApiReference("mine", "Coding assistant"));
            softly.assertThat(output.apis().getTotalElements()).as("APIs running the guardrail").isEqualTo(2);
        });
    }

    /**
     * A caller with no identified user holds no permission to check an API against, so it is named none of them: the
     * REST layer answers such an actor for a request whose principal carries no user, and the whole environment would
     * otherwise be listed past the per-API filter.
     */
    @Test
    void should_name_no_api_to_a_call_without_a_user() {
        guardrails.givenGuardrail(aGuardrail().build());
        var runningIt = Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build();
        apis.givenApi(aGuardedApi().id("api-1").name("Coding assistant").internalFlow(runningIt).build());
        var unnamed = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build();

        var output = useCase.execute(new ListGuardrailApisUseCase.Input(unnamed, GUARDRAIL_ID, 0, 0));

        assertSoftly(softly -> {
            softly.assertThat(output.apis().getContent()).as("APIs named to a caller with no user").isEmpty();
            softly.assertThat(output.apis().getTotalElements()).as("APIs running the guardrail, named or not").isEqualTo(1);
        });
    }

    @Test
    void should_refuse_a_guardrail_of_another_environment() {
        guardrails.givenGuardrail(aGuardrail(d -> d.environmentId("other-env")).build());

        assertThatThrownBy(() -> useCase.execute(new ListGuardrailApisUseCase.Input(actor, GUARDRAIL_ID, 0, 0)))
            .as("guardrail of another environment")
            .isInstanceOf(GuardrailNotFoundException.class);
    }
}
