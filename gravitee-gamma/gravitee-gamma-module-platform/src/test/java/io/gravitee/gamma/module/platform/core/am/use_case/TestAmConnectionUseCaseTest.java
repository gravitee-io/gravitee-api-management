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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.core.am.port.service_provider.AmConnectionTester;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.TestAmConnectionUseCase;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TestAmConnectionUseCaseTest {

    private AmConnectionRepository repository;
    private AmConnectionTester tester;
    private TestAmConnectionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AmConnectionRepository.class);
        tester = Mockito.mock(AmConnectionTester.class);
        useCase = new TestAmConnectionUseCase(repository, tester);
    }

    @Test
    void should_use_inbound_when_supplied_and_strip_trailing_slash() {
        when(repository.findByOrg("ORG")).thenReturn(Optional.empty());
        when(tester.test(Mockito.eq("ORG"), Mockito.any())).thenReturn(AmConnectionTestResult.success());

        useCase.execute(new TestAmConnectionUseCase.Input("ORG", "https://am.example/", "inbound-token"));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(tester).test(Mockito.eq("ORG"), captor.capture());
        assertThat(captor.getValue().baseUrl()).isEqualTo("https://am.example");
        assertThat(captor.getValue().serviceAccountAccessToken()).isEqualTo("inbound-token");
    }

    @Test
    void should_fall_back_to_stored_when_inbound_blank() {
        when(repository.findByOrg("ORG")).thenReturn(
            Optional.of(new AmConnection("https://stored.example", "stored-token", null, null, null))
        );
        when(tester.test(Mockito.eq("ORG"), Mockito.any())).thenReturn(AmConnectionTestResult.success());

        useCase.execute(new TestAmConnectionUseCase.Input("ORG", "", null));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(tester).test(Mockito.eq("ORG"), captor.capture());
        assertThat(captor.getValue().baseUrl()).isEqualTo("https://stored.example");
        assertThat(captor.getValue().serviceAccountAccessToken()).isEqualTo("stored-token");
    }

    @Test
    void should_treat_empty_inbound_token_as_clear() {
        when(repository.findByOrg("ORG")).thenReturn(
            Optional.of(new AmConnection("https://stored.example", "stored-token", null, null, null))
        );
        when(tester.test(Mockito.eq("ORG"), Mockito.any())).thenReturn(AmConnectionTestResult.failure(400, "no token"));

        useCase.execute(new TestAmConnectionUseCase.Input("ORG", "https://am.example", ""));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(tester).test(Mockito.eq("ORG"), captor.capture());
        assertThat(captor.getValue().serviceAccountAccessToken()).isNull();
    }
}
