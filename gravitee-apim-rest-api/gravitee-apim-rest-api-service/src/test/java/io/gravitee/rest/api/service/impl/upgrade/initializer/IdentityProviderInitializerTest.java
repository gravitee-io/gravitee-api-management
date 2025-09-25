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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderNotFoundException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class IdentityProviderInitializerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    private IdentityProviderInitializer initializer;

    private static final String GOOGLE_NAME = "google";
    private static final String GOOGLE_ID = IdGenerator.generate(GOOGLE_NAME);

    @Before
    public void setUp() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("security.providers[0].type", "google")
            .withProperty("security.providers[0].activations[0]", "DEFAULT")
            .withProperty("security.providers[0].activations[1]", "DEFAULT:DEFAULT")
            .withProperty("security.providers[1].type", "gravitee")
            .withProperty("security.providers[1].type", "unknown");

        initializer = new IdentityProviderInitializer(
            environment,
            groupService,
            organizationService,
            environmentService,
            identityProviderService,
            identityProviderActivationService
        );
    }

    @Test
    public void shouldCreateAndUpdateOneIdentityProvider() {
        when(identityProviderService.findById(GOOGLE_ID)).thenThrow(IdentityProviderNotFoundException.class);

        when(identityProviderService.create(any(ExecutionContext.class), any(NewIdentityProviderEntity.class))).thenReturn(googleIDP());

        when(organizationService.findById("DEFAULT")).thenReturn(defaultOrganization());

        when(environmentService.findById("DEFAULT")).thenReturn(defaultEnvironment());

        initializer.initialize();

        verify(identityProviderService, times(1)).create(any(ExecutionContext.class), argThat(idp -> idp.getName().equals(GOOGLE_ID)));

        verify(identityProviderService, times(1)).update(
            any(ExecutionContext.class),
            eq(GOOGLE_ID),
            argThat(idp -> idp.getName().equals(GOOGLE_ID))
        );

        verify(identityProviderActivationService, times(1)).deactivateIdpOnAllTargets(any(ExecutionContext.class), eq(GOOGLE_NAME));

        verify(identityProviderActivationService, times(1)).activateIdpOnTargets(
            any(ExecutionContext.class),
            eq(GOOGLE_NAME),
            eq(expectedActivationTargets())
        );
    }

    @Test
    public void shouldUpdateOneIdentityProvider() {
        when(identityProviderService.findById(GOOGLE_ID)).thenReturn(googleIDP());

        when(organizationService.findById("DEFAULT")).thenReturn(defaultOrganization());

        when(environmentService.findById("DEFAULT")).thenReturn(defaultEnvironment());

        initializer.initialize();

        verify(identityProviderService, never()).create(any(ExecutionContext.class), any());

        verify(identityProviderService, times(1)).update(
            any(ExecutionContext.class),
            eq(GOOGLE_ID),
            argThat(idp -> idp.getName().equals(GOOGLE_ID))
        );

        verify(identityProviderActivationService, times(1)).deactivateIdpOnAllTargets(any(ExecutionContext.class), eq(GOOGLE_NAME));

        verify(identityProviderActivationService, times(1)).activateIdpOnTargets(
            any(ExecutionContext.class),
            eq(GOOGLE_NAME),
            eq(expectedActivationTargets())
        );
    }

    private static ActivationTarget[] expectedActivationTargets() {
        return List.of(
            new ActivationTarget("DEFAULT", IdentityProviderActivationReferenceType.ORGANIZATION),
            new ActivationTarget("DEFAULT", IdentityProviderActivationReferenceType.ENVIRONMENT)
        ).toArray(new ActivationTarget[] {});
    }

    private static OrganizationEntity defaultOrganization() {
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId("DEFAULT");
        return organizationEntity;
    }

    private static EnvironmentEntity defaultEnvironment() {
        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId("DEFAULT");
        environmentEntity.setOrganizationId("DEFAULT");
        return environmentEntity;
    }

    private IdentityProviderEntity googleIDP() {
        IdentityProviderEntity idpEntity = new IdentityProviderEntity();
        idpEntity.setId(GOOGLE_ID);
        return idpEntity;
    }

    @Test
    public void testOrder() {
        Assert.assertEquals(InitializerOrder.IDENTITY_PROVIDER_INITIALIZER, initializer.getOrder());
    }
}
