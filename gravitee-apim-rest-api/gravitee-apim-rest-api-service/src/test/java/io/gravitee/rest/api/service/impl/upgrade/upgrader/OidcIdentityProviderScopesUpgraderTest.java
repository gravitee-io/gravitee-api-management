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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.OidcIdentityProviderScopesUpgrader.DEFAULT_SCOPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcIdentityProviderScopesUpgraderTest {

    @InjectMocks
    OidcIdentityProviderScopesUpgrader upgrader;

    @Mock
    IdentityProviderRepository identityProviderRepository;

    @Test
    void upgrade_should_backfill_scopes_when_scopes_key_absent() throws TechnicalException, UpgraderException {
        var idp = oidcIdp("idp-1", null);
        when(identityProviderRepository.findAll()).thenReturn(Set.of(idp));

        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(captor.capture());
        assertThat(captor.getValue().getConfiguration().get("scopes")).isEqualTo(DEFAULT_SCOPES);
    }

    @Test
    void upgrade_should_backfill_scopes_when_configuration_is_null() throws TechnicalException, UpgraderException {
        var idp = new IdentityProvider();
        idp.setId("idp-null-config");
        idp.setType(IdentityProviderType.OIDC);
        idp.setConfiguration(null);
        when(identityProviderRepository.findAll()).thenReturn(Set.of(idp));

        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(captor.capture());
        assertThat(captor.getValue().getConfiguration().get("scopes")).isEqualTo(DEFAULT_SCOPES);
    }

    @Test
    void upgrade_should_backfill_scopes_when_empty() throws TechnicalException, UpgraderException {
        var idp = oidcIdp("idp-2", List.of());
        when(identityProviderRepository.findAll()).thenReturn(Set.of(idp));

        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(captor.capture());
        assertThat(captor.getValue().getConfiguration().get("scopes")).isEqualTo(DEFAULT_SCOPES);
    }

    @Test
    void upgrade_should_not_overwrite_existing_scopes() throws TechnicalException, UpgraderException {
        var idp = oidcIdp("idp-3", List.of("openid"));
        when(identityProviderRepository.findAll()).thenReturn(Set.of(idp));

        assertThat(upgrader.upgrade()).isTrue();

        verify(identityProviderRepository, never()).update(idp);
    }

    @Test
    void upgrade_should_skip_non_oidc_providers() throws TechnicalException, UpgraderException {
        var idp = new IdentityProvider();
        idp.setId("idp-4");
        idp.setType(IdentityProviderType.GITHUB);
        idp.setConfiguration(new HashMap<>());
        when(identityProviderRepository.findAll()).thenReturn(Set.of(idp));

        assertThat(upgrader.upgrade()).isTrue();

        verify(identityProviderRepository, never()).update(idp);
    }

    @Test
    void upgrade_should_throw_on_exception() throws TechnicalException {
        when(identityProviderRepository.findAll()).thenThrow(new TechnicalException("db error"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    private IdentityProvider oidcIdp(String id, List<String> scopes) {
        var idp = new IdentityProvider();
        idp.setId(id);
        idp.setType(IdentityProviderType.OIDC);
        var config = new HashMap<String, Object>();
        if (scopes != null) {
            config.put("scopes", scopes);
        }
        idp.setConfiguration(config);
        return idp;
    }
}
