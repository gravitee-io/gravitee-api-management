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
package io.gravitee.gateway.reactive.platform.organization.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class OrganizationReactorRegistryTest {

    public static final String ORGANIZATION_ID = "id";
    OrganizationReactorRegistry cut;

    @Mock
    private OrganizationReactorFactory organizationReactorFactory;

    @Mock
    private OrganizationReactor organizationReactor;

    @BeforeEach
    public void beforeEach() {
        cut = new OrganizationReactorRegistry(organizationReactorFactory);
        lenient().when(organizationReactor.id()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    void should_create_and_start_organization_reactor() throws Exception {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenReturn(organizationReactor);

        // When
        cut.create(reactableOrganization);

        // Then
        verify(organizationReactorFactory).create(reactableOrganization);
        verify(organizationReactor).start();
    }

    @Test
    void should_register_a_newly_created_and_started_organization_reactor() {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenReturn(organizationReactor);

        // When
        cut.create(reactableOrganization);

        // Then
        OrganizationReactor organizationReactor = cut.get(ORGANIZATION_ID);
        assertThat(organizationReactor).isEqualTo(this.organizationReactor);
    }

    @Test
    void should_ignore_failure_when_creating_organization_reactor() {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenThrow(new RuntimeException());

        // When/Then
        Assertions.assertDoesNotThrow(() -> cut.create(reactableOrganization));
    }

    @Test
    void should_remove_a_created_and_started_organization_reactor() throws Exception {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenReturn(organizationReactor);
        cut.create(reactableOrganization);

        // When
        cut.remove(reactableOrganization);

        // Then
        verify(organizationReactor).stop();
        OrganizationReactor organizationReactor = cut.get(ORGANIZATION_ID);
        assertThat(organizationReactor).isNull();
    }

    @Test
    void should_ignore_failure_when_removing_a_created_and_started_organization_reactor() throws Exception {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenReturn(organizationReactor);
        when(organizationReactor.stop()).thenThrow(new RuntimeException());
        cut.create(reactableOrganization);

        // When
        cut.remove(reactableOrganization);

        // Then
        verify(organizationReactor).stop();
        OrganizationReactor organizationReactor = cut.get(ORGANIZATION_ID);
        assertThat(organizationReactor).isNull();
    }

    @Test
    void should_clear_created_and_started_organization_reactor() throws Exception {
        // Given
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        ReactableOrganization reactableOrganization = new ReactableOrganization(organization);
        when(organizationReactorFactory.create(reactableOrganization)).thenReturn(organizationReactor);
        when(organizationReactor.stop()).thenThrow(new RuntimeException());
        cut.create(reactableOrganization);

        // When
        cut.clear();

        // Then
        verify(organizationReactor).stop();
        OrganizationReactor organizationReactor = cut.get(ORGANIZATION_ID);
        assertThat(organizationReactor).isNull();
    }
}
