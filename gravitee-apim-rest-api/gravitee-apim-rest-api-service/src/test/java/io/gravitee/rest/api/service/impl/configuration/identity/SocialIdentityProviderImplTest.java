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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.configuration.identity.ClientAuthenticationMethod;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the collapse of the free-form {@code tokenEndpointAuthMethod} configuration string into the typed enum, and
 * that a disabled IdP is rejected for environment (Portal) login while staying available for organization (Console) login.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SocialIdentityProviderImplTest {

    private static final String PROVIDER_ID = "oidc-provider";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ORGANIZATION_ID);
    private static final ActivationTarget ORGANIZATION_TARGET = new ActivationTarget(
        ORGANIZATION_ID,
        IdentityProviderActivationReferenceType.ORGANIZATION
    );
    private static final ActivationTarget ENVIRONMENT_TARGET = new ActivationTarget(
        ORGANIZATION_ID,
        IdentityProviderActivationReferenceType.ENVIRONMENT
    );

    @Mock
    private IdentityProviderService identityProviderService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    @InjectMocks
    private SocialIdentityProviderImpl socialIdentityProvider;

    private final Map<String, Object> configuration = new HashMap<>();

    @BeforeEach
    void setUp() {
        configuration.clear();
        configuration.put("clientId", "the_client_id");
        configuration.put("clientSecret", "the_client_secret");
        configuration.put("tokenEndpoint", "https://provider.example.com/token");
    }

    @ParameterizedTest
    @CsvSource({ "client_secret_basic, CLIENT_SECRET_BASIC", "client_secret_post, CLIENT_SECRET_POST" })
    void should_resolve_the_configured_client_authentication_method(String configured, ClientAuthenticationMethod expected) {
        configuration.put("tokenEndpointAuthMethod", configured);

        assertThat(findProvider().getTokenEndpointAuthMethod()).isEqualTo(expected);
    }

    @Test
    void should_leave_the_method_unset_when_the_provider_declares_none() {
        assertThat(findProvider().getTokenEndpointAuthMethod()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "client_secret_jwt", "none", "typo", "  " })
    void should_fall_back_to_the_endpoint_defaults_on_an_unusable_value(String configured) {
        configuration.put("tokenEndpointAuthMethod", configured);

        // Falling back rather than failing is the safety property: a typo must not stop every login for this provider
        assertThat(findProvider().getTokenEndpointAuthMethod()).isNull();
    }

    @Test
    void should_fall_back_rather_than_fail_when_the_configured_value_is_not_a_string() {
        // The configuration map is free-form, so an unquoted value arrives as a number or boolean. convert() is wrapped
        // by findAll() into a single failure, so throwing here would remove every provider from the list, not just this one.
        configuration.put("tokenEndpointAuthMethod", 123);

        assertThatCode(() -> assertThat(findProvider().getTokenEndpointAuthMethod()).isNull()).doesNotThrowAnyException();
    }

    @Test
    void find_all_should_include_disabled_idp_for_organization_target() {
        givenActivatedIdps(ORGANIZATION_TARGET, googleIdp("enabled-idp", "Enabled", true), googleIdp("disabled-idp", "Disabled", false));

        assertThat(socialIdentityProvider.findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET))
            .extracting(SocialIdentityProviderEntity::getId)
            .containsExactlyInAnyOrder("enabled-idp", "disabled-idp");
    }

    @Test
    void find_all_should_exclude_idp_not_activated_on_organization_target() {
        IdentityProviderEntity activated = googleIdp("activated-idp", "Activated", true);
        IdentityProviderEntity deactivated = googleIdp("deactivated-idp", "Deactivated", true);
        when(identityProviderActivationService.findAllByTarget(ORGANIZATION_TARGET)).thenReturn(Set.of(activation(activated.getId())));
        when(identityProviderService.findAll(EXECUTION_CONTEXT)).thenReturn(Set.of(activated, deactivated));

        assertThat(socialIdentityProvider.findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET))
            .extracting(SocialIdentityProviderEntity::getId)
            .containsExactly("activated-idp");
    }

    @Test
    void find_all_should_exclude_disabled_idp_for_environment_target() {
        givenActivatedIdps(ENVIRONMENT_TARGET, googleIdp("enabled-idp", "Enabled", true), googleIdp("disabled-idp", "Disabled", false));

        assertThat(socialIdentityProvider.findAll(EXECUTION_CONTEXT, ENVIRONMENT_TARGET))
            .extracting(SocialIdentityProviderEntity::getId)
            .containsExactly("enabled-idp");
    }

    @Test
    void find_all_should_include_enabled_idp_for_organization_target() {
        givenActivatedIdps(ORGANIZATION_TARGET, googleIdp("enabled-idp", "Enabled", true));

        assertThat(socialIdentityProvider.findAll(EXECUTION_CONTEXT, ORGANIZATION_TARGET))
            .extracting(SocialIdentityProviderEntity::getId)
            .containsExactly("enabled-idp");
    }

    @Test
    void find_by_id_should_return_enabled_idp_for_organization_target() {
        givenActivatedIdp(ORGANIZATION_TARGET, googleIdp("enabled-idp", "Enabled", true));

        assertThat(socialIdentityProvider.findById("enabled-idp", ORGANIZATION_TARGET).getId()).isEqualTo("enabled-idp");
    }

    @Test
    void find_by_id_should_return_disabled_idp_for_organization_target() {
        givenActivatedIdp(ORGANIZATION_TARGET, googleIdp("disabled-idp", "Disabled", false));

        assertThat(socialIdentityProvider.findById("disabled-idp", ORGANIZATION_TARGET).getId()).isEqualTo("disabled-idp");
    }

    @Test
    void find_by_id_should_reject_disabled_idp_for_environment_target() {
        givenActivatedIdp(ENVIRONMENT_TARGET, googleIdp("disabled-idp", "Disabled", false));

        assertThatThrownBy(() -> socialIdentityProvider.findById("disabled-idp", ENVIRONMENT_TARGET)).isInstanceOf(
            IdentityProviderNotFoundException.class
        );
    }

    @Test
    void find_by_id_should_reject_idp_not_activated_on_target() {
        when(identityProviderActivationService.findAllByTarget(ORGANIZATION_TARGET)).thenReturn(Set.of());

        assertThatThrownBy(() -> socialIdentityProvider.findById("missing-idp", ORGANIZATION_TARGET)).isInstanceOf(
            IdentityProviderNotFoundException.class
        );
    }

    private SocialIdentityProviderEntity findProvider() {
        IdentityProviderEntity identityProvider = new IdentityProviderEntity();
        identityProvider.setId(PROVIDER_ID);
        identityProvider.setName("OIDC Provider");
        identityProvider.setType(IdentityProviderType.OIDC);
        identityProvider.setEnabled(true);
        identityProvider.setConfiguration(configuration);

        IdentityProviderActivationEntity activation = new IdentityProviderActivationEntity();
        activation.setIdentityProvider(PROVIDER_ID);

        when(identityProviderActivationService.findAllByTarget(any())).thenReturn(Set.of(activation));
        when(identityProviderService.findById(PROVIDER_ID)).thenReturn(identityProvider);

        return socialIdentityProvider.findById(
            PROVIDER_ID,
            new IdentityProviderActivationService.ActivationTarget(ORGANIZATION_ID, IdentityProviderActivationReferenceType.ORGANIZATION)
        );
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
