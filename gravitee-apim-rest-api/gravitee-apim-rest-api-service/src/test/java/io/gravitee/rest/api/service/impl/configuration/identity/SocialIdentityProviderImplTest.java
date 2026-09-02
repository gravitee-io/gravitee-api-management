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
package io.gravitee.rest.api.service.impl.configuration.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SocialIdentityProviderImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");
    private static final ActivationTarget ORGANIZATION_TARGET = new ActivationTarget(
        "DEFAULT",
        IdentityProviderActivationReferenceType.ORGANIZATION
    );
    private static final ActivationTarget ENVIRONMENT_TARGET = new ActivationTarget(
        "DEFAULT",
        IdentityProviderActivationReferenceType.ENVIRONMENT
    );

    @InjectMocks
    private SocialIdentityProviderImpl socialIdentityProvider = new SocialIdentityProviderImpl();

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @Test
    public void find_all_should_include_disabled_idp_for_organization_target() {
        givenActivatedIdps(ORGANIZATION_TARGET, googleIdp("enabled-idp", "Enabled", true), googleIdp("disabled-idp", "Disabled", false));

        Set<String> ids = socialIdentityProvider
            .findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET)
            .stream()
            .map(SocialIdentityProviderEntity::getId)
            .collect(Collectors.toSet());

        assertEquals(Set.of("enabled-idp", "disabled-idp"), ids);
    }

    @Test
    public void find_all_should_exclude_idp_not_activated_on_organization_target() {
        IdentityProviderEntity activated = googleIdp("activated-idp", "Activated", true);
        IdentityProviderEntity deactivated = googleIdp("deactivated-idp", "Deactivated", true);
        when(identityProviderActivationService.findAllByTarget(ORGANIZATION_TARGET)).thenReturn(Set.of(activation(activated.getId())));
        when(identityProviderService.findAll(EXECUTION_CONTEXT)).thenReturn(Set.of(activated, deactivated));

        Set<String> ids = socialIdentityProvider
            .findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET)
            .stream()
            .map(SocialIdentityProviderEntity::getId)
            .collect(Collectors.toSet());

        assertEquals(Set.of("activated-idp"), ids);
    }

    @Test
    public void find_all_should_exclude_disabled_idp_for_environment_target() {
        givenActivatedIdps(ENVIRONMENT_TARGET, googleIdp("enabled-idp", "Enabled", true), googleIdp("disabled-idp", "Disabled", false));

        Set<String> ids = socialIdentityProvider
            .findAll(EXECUTION_CONTEXT, ENVIRONMENT_TARGET)
            .stream()
            .map(SocialIdentityProviderEntity::getId)
            .collect(Collectors.toSet());

        assertEquals(Set.of("enabled-idp"), ids);
    }

    @Test
    public void find_by_id_should_return_enabled_idp_for_organization_target() {
        IdentityProviderEntity enabled = googleIdp("enabled-idp", "Enabled", true);
        givenActivatedIdp(ORGANIZATION_TARGET, enabled);

        SocialIdentityProviderEntity result = socialIdentityProvider.findById("enabled-idp", ORGANIZATION_TARGET);

        assertEquals("enabled-idp", result.getId());
    }

    @Test
    public void find_by_id_should_return_disabled_idp_for_organization_target() {
        givenActivatedIdp(ORGANIZATION_TARGET, googleIdp("disabled-idp", "Disabled", false));

        SocialIdentityProviderEntity result = socialIdentityProvider.findById("disabled-idp", ORGANIZATION_TARGET);

        assertEquals("disabled-idp", result.getId());
    }

    @Test(expected = IdentityProviderNotFoundException.class)
    public void find_by_id_should_reject_disabled_idp_for_environment_target() {
        givenActivatedIdp(ENVIRONMENT_TARGET, googleIdp("disabled-idp", "Disabled", false));

        socialIdentityProvider.findById("disabled-idp", ENVIRONMENT_TARGET);
    }

    @Test(expected = IdentityProviderNotFoundException.class)
    public void find_by_id_should_reject_idp_not_activated_on_target() {
        when(identityProviderActivationService.findAllByTarget(ORGANIZATION_TARGET)).thenReturn(Set.of());

        socialIdentityProvider.findById("missing-idp", ORGANIZATION_TARGET);
    }

    @Test
    public void find_all_should_include_enabled_idp_for_organization_target() {
        givenActivatedIdps(ORGANIZATION_TARGET, googleIdp("enabled-idp", "Enabled", true));

        Set<SocialIdentityProviderEntity> result = socialIdentityProvider.findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET);

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(idp -> "enabled-idp".equals(idp.getId())));
    }

    private void givenActivatedIdps(ActivationTarget target, IdentityProviderEntity... identityProviders) {
        Set<IdentityProviderActivationEntity> activations = Set.of(identityProviders)
            .stream()
            .map(idp -> activation(idp.getId()))
            .collect(Collectors.toSet());
        when(identityProviderActivationService.findAllByTarget(target)).thenReturn(activations);
        when(identityProviderService.findAll(EXECUTION_CONTEXT)).thenReturn(Set.of(identityProviders));
    }

    private void givenActivatedIdp(ActivationTarget target, IdentityProviderEntity identityProvider) {
        when(identityProviderActivationService.findAllByTarget(target)).thenReturn(Set.of(activation(identityProvider.getId())));
        when(identityProviderService.findById(identityProvider.getId())).thenReturn(identityProvider);
    }

    private static IdentityProviderActivationEntity activation(String identityProviderId) {
        IdentityProviderActivationEntity activation = new IdentityProviderActivationEntity();
        activation.setIdentityProvider(identityProviderId);
        return activation;
    }

    private static IdentityProviderEntity googleIdp(String id, String name, boolean enabled) {
        IdentityProviderEntity identityProvider = new IdentityProviderEntity();
        identityProvider.setId(id);
        identityProvider.setName(name);
        identityProvider.setType(IdentityProviderType.GOOGLE);
        identityProvider.setEnabled(enabled);
        identityProvider.setConfiguration(Map.of("clientId", "client-id", "clientSecret", "client-secret"));
        return identityProvider;
    }
}
