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
import io.gravitee.gamma.module.platform.core.am.domain_service.AmConnectionViewDomainService;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.SaveAmConnectionUseCase;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SaveAmConnectionUseCaseTest {

    private AmConnectionRepository repository;
    private SaveAmConnectionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AmConnectionRepository.class);
        useCase = new SaveAmConnectionUseCase(repository, new AmConnectionViewDomainService(repository));
    }

    @Test
    void should_persist_connection_with_all_input_fields() {
        when(repository.findByOrg("ORG")).thenReturn(Optional.empty());
        when(repository.hasTokenForOrg("ORG")).thenReturn(true);

        useCase.execute(
            new SaveAmConnectionUseCase.Input("ORG", "https://am.example", "secret-token", "domain-1", "my-domain", "https://gw.example")
        );

        var saved = ArgumentCaptor.forClass(AmConnection.class);
        Mockito.verify(repository).save(Mockito.eq("ORG"), saved.capture());
        assertThat(saved.getValue()).isEqualTo(
            new AmConnection("https://am.example", "secret-token", "domain-1", "my-domain", "https://gw.example")
        );
    }

    @Test
    void should_return_readback_view_with_token_flagged_present() {
        when(repository.findByOrg("ORG")).thenReturn(
            Optional.of(new AmConnection("https://am.example", "secret-token", "domain-1", "my-domain", "https://gw.example"))
        );
        when(repository.hasTokenForOrg("ORG")).thenReturn(true);

        var output = useCase.execute(
            new SaveAmConnectionUseCase.Input("ORG", "https://am.example", "secret-token", "domain-1", "my-domain", "https://gw.example")
        );

        assertThat(output.baseUrl()).isEqualTo("https://am.example");
        assertThat(output.hasAccessToken()).isTrue();
        assertThat(output.defaultDomainId()).isEqualTo("domain-1");
        assertThat(output.defaultDomainHrid()).isEqualTo("my-domain");
        assertThat(output.gatewayUrl()).isEqualTo("https://gw.example");
    }

    @Test
    void should_report_no_token_when_blank_token_clears_it() {
        // Blank token clears the saved ciphertext (see AmConnectionRepository#save semantics),
        // so the read-back reports the connection present but no token.
        when(repository.findByOrg("ORG")).thenReturn(Optional.of(new AmConnection("https://am.example", null, null, null, null)));
        when(repository.hasTokenForOrg("ORG")).thenReturn(false);

        var output = useCase.execute(new SaveAmConnectionUseCase.Input("ORG", "https://am.example", "", null, null, null));

        assertThat(output.baseUrl()).isEqualTo("https://am.example");
        assertThat(output.hasAccessToken()).isFalse();
    }
}
