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
package io.gravitee.gateway.services.sync.process.repository.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentServiceTest {

    private static final String ENV_ID = "env";
    private static final String ORG_ID = "org";

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ReactableApi<?> reactableApi;

    private EnvironmentService cut;

    @BeforeEach
    void setUp() {
        cut = new EnvironmentService(environmentRepository, organizationRepository);
    }

    private Environment environment() {
        Environment environment = new Environment();
        environment.setId(ENV_ID);
        environment.setOrganizationId(ORG_ID);
        return environment;
    }

    private Organization organization() {
        Organization organization = new Organization();
        organization.setId(ORG_ID);
        return organization;
    }

    @Test
    void should_fill_environment_and_organization() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(environment()));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization()));

        cut.fill(ENV_ID, reactableApi);

        verify(reactableApi).setEnvironmentId(ENV_ID);
        verify(reactableApi).setOrganizationId(ORG_ID);
    }

    @Test
    void should_cache_environment_and_organization_and_not_refetch_on_subsequent_calls() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(environment()));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization()));

        cut.fill(ENV_ID, reactableApi);
        cut.fill(ENV_ID, reactableApi);

        verify(environmentRepository, times(1)).findById(ENV_ID);
        verify(organizationRepository, times(1)).findById(ORG_ID);
        verify(reactableApi, times(2)).setOrganizationId(ORG_ID);
    }

    @Test
    void should_leave_organization_null_when_organization_is_absent() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(environment()));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        cut.fill(ENV_ID, reactableApi);

        verify(reactableApi).setEnvironmentId(ENV_ID);
        verify(reactableApi, never()).setOrganizationId(ORG_ID);
    }

    @Test
    void should_propagate_when_organization_fetch_fails_transiently() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(environment()));
        when(organizationRepository.findById(ORG_ID)).thenThrow(new TechnicalException("bridge 502"));

        assertThatThrownBy(() -> cut.fill(ENV_ID, reactableApi)).isInstanceOf(SyncException.class);
    }

    @Test
    void should_not_cache_environment_when_organization_fetch_fails() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(environment()));
        // first fetch fails transiently, second fetch succeeds (bridge recovered)
        when(organizationRepository.findById(ORG_ID))
            .thenThrow(new TechnicalException("bridge 502"))
            .thenReturn(Optional.of(organization()));

        assertThatThrownBy(() -> cut.fill(ENV_ID, reactableApi)).isInstanceOf(SyncException.class);

        // a subsequent sync must re-resolve the organization (no poisoned partial cache)
        cut.fill(ENV_ID, reactableApi);

        verify(reactableApi).setOrganizationId(ORG_ID);
    }

    @Test
    void should_propagate_when_environment_fetch_fails_transiently() throws TechnicalException {
        when(environmentRepository.findById(ENV_ID)).thenThrow(new TechnicalException("bridge 502"));

        assertThatThrownBy(() -> cut.fill(ENV_ID, reactableApi)).isInstanceOf(SyncException.class);
    }
}
