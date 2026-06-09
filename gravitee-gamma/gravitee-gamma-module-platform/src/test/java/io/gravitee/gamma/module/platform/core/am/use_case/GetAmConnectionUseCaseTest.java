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
package io.gravitee.gamma.module.platform.core.am.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.GetAmConnectionUseCase;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAmConnectionUseCaseTest {

    private AmConnectionRepository repository;
    private GetAmConnectionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AmConnectionRepository.class);
        useCase = new GetAmConnectionUseCase(repository);
    }

    @Test
    void should_return_empty_baseUrl_and_no_token_when_nothing_saved() {
        when(repository.findByOrg("ORG")).thenReturn(Optional.empty());
        when(repository.hasTokenForOrg("ORG")).thenReturn(false);

        var output = useCase.execute(new GetAmConnectionUseCase.Input("ORG"));

        assertThat(output.baseUrl()).isEmpty();
        assertThat(output.hasAccessToken()).isFalse();
    }

    @Test
    void should_report_token_present_even_when_decrypt_failed() {
        when(repository.findByOrg("ORG")).thenReturn(Optional.of(new AmConnection("https://am.example", null, null, null, null)));
        when(repository.hasTokenForOrg("ORG")).thenReturn(true);

        var output = useCase.execute(new GetAmConnectionUseCase.Input("ORG"));

        assertThat(output.baseUrl()).isEqualTo("https://am.example");
        assertThat(output.hasAccessToken()).isTrue();
    }
}
