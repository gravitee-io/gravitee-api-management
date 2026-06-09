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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.module.platform.core.am.exception.AmUpstreamException;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.Domain;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmDirectoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetDomainUseCaseTest {

    private AmDirectoryClient amDirectoryClient;
    private GetDomainUseCase useCase;

    @BeforeEach
    void setUp() {
        amDirectoryClient = Mockito.mock(AmDirectoryClient.class);
        useCase = new GetDomainUseCase(amDirectoryClient);
    }

    @Test
    void should_return_the_domain_from_the_port() {
        Domain domain = new Domain("domain-id", "My Domain", "my-domain");
        when(amDirectoryClient.getDomain("O", "E", "domain-id")).thenReturn(domain);

        GetDomainUseCase.Output output = useCase.execute(new GetDomainUseCase.Input("O", "E", "domain-id"));

        assertThat(output.domain()).isEqualTo(domain);
        verify(amDirectoryClient).getDomain("O", "E", "domain-id");
    }

    @Test
    void should_propagate_upstream_errors() {
        when(amDirectoryClient.getDomain("O", "E", "missing")).thenThrow(new AmUpstreamException("not found", 404));

        assertThatThrownBy(() -> useCase.execute(new GetDomainUseCase.Input("O", "E", "missing"))).isInstanceOf(AmUpstreamException.class);
    }
}
