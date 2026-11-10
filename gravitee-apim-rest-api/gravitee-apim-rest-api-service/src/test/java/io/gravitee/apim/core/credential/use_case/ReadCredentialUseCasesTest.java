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
package io.gravitee.gamma.module.platform.core.credential.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialNotFoundException;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialConsumer;
import io.gravitee.gamma.module.platform.fixture.CredentialFixture;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReadCredentialUseCasesTest extends AbstractUseCaseTest {

    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new RootFixture().credentialFixture;
    }

    @Nested
    class Get {

        @Test
        void should_return_a_credential_to_any_reader() {
            var credential = fixture.givenACredential();

            var found = fixture.getCredentialUseCase.execute(new GetCredentialUseCase.Input(fixture.otherUser(), credential.id()));

            assertThat(found.credential()).isEqualTo(credential);
        }

        @Test
        void should_not_find_a_credential_of_another_environment() {
            var credential = fixture.givenACredential();

            assertThatThrownBy(() ->
                fixture.getCredentialUseCase.execute(
                    new GetCredentialUseCase.Input(CredentialFixture.adminIn(CredentialFixture.ORG_ID, "env-2"), credential.id())
                )
            ).isInstanceOf(CredentialNotFoundException.class);
        }
    }

    @Nested
    class Search {

        @Test
        void should_page_the_credentials_of_the_environment_sorted_by_name() {
            fixture.givenACredential(c -> c.name("Cohere"));
            fixture.givenACredential(c -> c.name("Anthropic"));
            fixture.givenACredential(c -> c.name("Bedrock"));

            var page = fixture.searchCredentialsUseCase.execute(new SearchCredentialsUseCase.Input(fixture.owner(), null, 0, 2)).page();

            assertThat(page.getContent()).extracting(Credential::name).containsExactly("Anthropic", "Bedrock");
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        void should_filter_on_the_name() {
            fixture.givenACredential(c -> c.name("OpenAI production"));
            fixture.givenACredential(c -> c.name("Anthropic"));

            var page = fixture.searchCredentialsUseCase
                .execute(new SearchCredentialsUseCase.Input(fixture.owner(), "openai", 0, 10))
                .page();

            assertThat(page.getContent()).extracting(Credential::name).containsExactly("OpenAI production");
        }

        @Test
        void should_not_return_credentials_of_another_environment() {
            fixture.givenACredential();

            var page = fixture.searchCredentialsUseCase
                .execute(new SearchCredentialsUseCase.Input(CredentialFixture.adminIn(CredentialFixture.ORG_ID, "env-2"), null, 0, 10))
                .page();

            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    class List_consumers {

        @Test
        void should_return_the_consumers_every_module_reports() {
            var credential = fixture.givenACredential();
            var consumer = new CredentialConsumer("aim", "LLM_PROXY", "proxy-1", "Support bot proxy");
            fixture.consumers.given(CredentialFixture.ENV_ID, credential.id(), consumer);

            var consumers = fixture.listCredentialConsumersUseCase
                .execute(new ListCredentialConsumersUseCase.Input(fixture.otherUser(), credential.id()))
                .consumers();

            assertThat(consumers).containsExactly(consumer);
        }

        @ParameterizedTest
        @CsvSource({ "org-id,env-2", "other-org,env-id" })
        void should_not_find_a_credential_of_another_organization_or_environment(String organizationId, String environmentId) {
            var credential = fixture.givenACredential();

            assertThatThrownBy(() ->
                fixture.listCredentialConsumersUseCase.execute(
                    new ListCredentialConsumersUseCase.Input(CredentialFixture.adminIn(organizationId, environmentId), credential.id())
                )
            ).isInstanceOf(CredentialNotFoundException.class);
        }

        @Test
        void should_fail_when_the_credential_does_not_exist() {
            assertThatThrownBy(() ->
                fixture.listCredentialConsumersUseCase.execute(new ListCredentialConsumersUseCase.Input(fixture.owner(), "missing"))
            ).isInstanceOf(CredentialNotFoundException.class);
        }
    }
}
