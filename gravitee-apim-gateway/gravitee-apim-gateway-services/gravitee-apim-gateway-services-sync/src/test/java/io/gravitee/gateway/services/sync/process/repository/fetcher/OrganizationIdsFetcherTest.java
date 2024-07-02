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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Installation;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OrganizationIdsFetcherTest {

    public static final Set<String> ENVS = Set.of("env1");

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private OrganizationIdsFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new OrganizationIdsFetcher(environmentRepository, gatewayConfiguration);
    }

    @Test
    void should_not_fetch_organization_if_hrid_enabled() {
        when(gatewayConfiguration.useLegacyEnvironmentHrids()).thenReturn(true);
        cut.fetch(ENVS).test().assertNoValues();
        verifyNoInteractions(environmentRepository);
    }

    @Test
    void should_fetch_organization_if_hrid_disable() throws TechnicalException {
        when(gatewayConfiguration.useLegacyEnvironmentHrids()).thenReturn(false);
        when(environmentRepository.findOrganizationIdsByEnvironments(ENVS)).thenReturn(Set.of("orga1"));
        cut.fetch(ENVS).test().assertValueCount(1).assertValue(events -> events.contains("orga1"));
    }

    @Test
    void should_return_empty_without_organization() throws TechnicalException {
        when(gatewayConfiguration.useLegacyEnvironmentHrids()).thenReturn(false);
        when(environmentRepository.findOrganizationIdsByEnvironments(ENVS)).thenReturn(null);
        cut.fetch(ENVS).test().assertNoValues();
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() throws TechnicalException {
        when(gatewayConfiguration.useLegacyEnvironmentHrids()).thenReturn(false);
        when(environmentRepository.findOrganizationIdsByEnvironments(ENVS)).thenThrow(new RuntimeException());
        cut.fetch(ENVS).test().assertError(RuntimeException.class);
    }
}
