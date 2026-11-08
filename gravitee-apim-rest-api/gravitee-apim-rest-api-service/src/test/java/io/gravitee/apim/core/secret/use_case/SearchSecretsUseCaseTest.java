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
package io.gravitee.apim.core.secret.use_case;

import static fixtures.core.model.SecretFixtures.aSecret;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.AbstractUseCaseTest;
import inmemory.SecretQueryServiceInMemory;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchSecretsUseCaseTest extends AbstractUseCaseTest {

    private final SecretQueryServiceInMemory secretQueryService = new SecretQueryServiceInMemory();

    private SearchSecretsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchSecretsUseCase(secretQueryService);
    }

    @Test
    void should_page_the_secrets_of_the_environment_sorted_by_key() {
        secretQueryService.initWith(
            List.of(
                aSecret().toBuilder().id("s1").key("mistral-key").build(),
                aSecret().toBuilder().id("s2").key("anthropic-key").build(),
                aSecret().toBuilder().id("s3").key("cohere-key").build(),
                aSecret().toBuilder().id("s4").key("other-env-key").environmentId("env-2").build()
            )
        );

        var first = useCase.execute(search(ORG_ID, 1, 2)).secrets();
        var second = useCase.execute(search(ORG_ID, 2, 2)).secrets();

        assertThat(first.getContent()).extracting(Secret::key).containsExactly("anthropic-key", "cohere-key");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(Secret::key).containsExactly("mistral-key");
        assertThat(second.getPageNumber()).isEqualTo(2);
    }

    @Test
    void should_return_an_empty_page_for_another_organization() {
        secretQueryService.initWith(List.of(aSecret()));

        var page = useCase.execute(search("org-2", 1, 10)).secrets();

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private SearchSecretsUseCase.Input search(String organizationId, int pageNumber, int pageSize) {
        return new SearchSecretsUseCase.Input(organizationId, ENV_ID, new PageableImpl(pageNumber, pageSize));
    }
}
