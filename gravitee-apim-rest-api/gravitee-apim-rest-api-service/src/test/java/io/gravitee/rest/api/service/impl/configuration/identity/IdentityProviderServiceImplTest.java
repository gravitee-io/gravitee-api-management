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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.UpdateIdentityProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IdentityProviderServiceImplTest {

    private static final String STORED_SECRET = "stored-client-secret";

    @InjectMocks
    private IdentityProviderServiceImpl identityProviderService = new IdentityProviderServiceImpl();

    @Mock
    private IdentityProviderRepository identityProviderRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private RoleService roleService;

    @Mock
    private IdentityProviderActivationService identityProviderActivationService;

    private IdentityProvider existingIdp(List<String> whitelist) {
        IdentityProvider idp = new IdentityProvider();
        idp.setId("idp-1");
        idp.setOrganizationId("DEFAULT");
        idp.setType(IdentityProviderType.OIDC);
        idp.setPersistedClaimsWhitelist(whitelist);
        return idp;
    }

    private IdentityProvider captureUpdated() throws Exception {
        ArgumentCaptor<IdentityProvider> captor = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(captor.capture());
        return captor.getValue();
    }

    @Test
    public void update_should_preserve_whitelist_when_payload_omits_it() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        // persistedClaimsWhitelist intentionally left null (omitted by a console save unaware of the field)

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of("org_id"), captureUpdated().getPersistedClaimsWhitelist());
    }

    @Test
    public void update_should_overwrite_whitelist_when_provided() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        update.setPersistedClaimsWhitelist(List.of("tenant"));

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of("tenant"), captureUpdated().getPersistedClaimsWhitelist());
    }

    @Test
    public void update_should_clear_whitelist_when_empty_list_provided() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(existingIdp(List.of("org_id"))));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My IdP");
        update.setPersistedClaimsWhitelist(List.of());

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        assertEquals(List.of(), captureUpdated().getPersistedClaimsWhitelist());
    }

    @Test
    public void findById_should_keep_client_secret_for_in_process_callers() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedOidcProvider()));

        IdentityProviderEntity entity = identityProviderService.findById("idp-1");

        assertThat(entity.getConfiguration()).containsEntry("clientId", "client-id").containsEntry("clientSecret", STORED_SECRET);
    }

    @Test
    public void create_should_store_client_secret_and_keep_it_for_in_process_callers() throws Exception {
        when(identityProviderRepository.findById(any())).thenReturn(Optional.empty());
        when(identityProviderRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NewIdentityProviderEntity create = new NewIdentityProviderEntity();
        create.setName("My OIDC");
        create.setType(io.gravitee.rest.api.model.configuration.identity.IdentityProviderType.OIDC);
        create.setConfiguration(Map.of("clientId", "client-id", "clientSecret", "new-secret"));

        IdentityProviderEntity created = identityProviderService.create(GraviteeContext.getExecutionContext(), create);

        ArgumentCaptor<IdentityProvider> stored = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).create(stored.capture());
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", "new-secret");
        assertThat(created.getConfiguration()).containsEntry("clientId", "client-id").containsEntry("clientSecret", "new-secret");

        IdentityProvider audited = auditedProvider(true);
        assertThat(audited.getConfiguration()).containsEntry("clientId", "client-id").doesNotContainKey("clientSecret");
        assertThat(audited.getName()).isEqualTo(stored.getValue().getName());
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", "new-secret");
    }

    @Test
    public void update_should_preserve_stored_client_secret_when_omitted() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedOidcProvider()));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My OIDC");
        update.setConfiguration(new HashMap<>(Map.of("clientId", "client-id")));

        IdentityProviderEntity result = identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        ArgumentCaptor<IdentityProvider> stored = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(stored.capture());
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", STORED_SECRET);
        assertThat(result.getConfiguration()).containsEntry("clientSecret", STORED_SECRET);

        IdentityProvider audited = auditedProvider(true);
        assertThat(audited.getConfiguration()).containsEntry("clientId", "client-id").doesNotContainKey("clientSecret");
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", STORED_SECRET);
    }

    @Test
    public void update_should_preserve_stored_client_secret_when_blank() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedOidcProvider()));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My OIDC");
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("clientId", "client-id");
        configuration.put("clientSecret", "  ");
        update.setConfiguration(configuration);

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        ArgumentCaptor<IdentityProvider> stored = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(stored.capture());
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", STORED_SECRET);
    }

    @Test
    public void update_should_replace_client_secret_when_provided() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedOidcProvider()));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My OIDC");
        update.setConfiguration(Map.of("clientId", "client-id", "clientSecret", "rotated-secret"));

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        ArgumentCaptor<IdentityProvider> stored = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(stored.capture());
        assertThat(stored.getValue().getConfiguration()).containsEntry("clientSecret", "rotated-secret");
    }

    @Test
    public void update_should_clear_configuration_when_incoming_configuration_is_null() throws Exception {
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedOidcProvider()));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My OIDC");
        update.setConfiguration(null);

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        ArgumentCaptor<IdentityProvider> stored = ArgumentCaptor.forClass(IdentityProvider.class);
        verify(identityProviderRepository).update(stored.capture());
        assertThat(stored.getValue().getConfiguration()).isNull();
    }

    @Test
    public void update_should_omit_client_secret_from_audit_without_changing_the_stored_provider() throws Exception {
        IdentityProvider storedProvider = storedOidcProvider();
        storedProvider.setDescription("kept");
        storedProvider.setEnabled(true);
        storedProvider.setEmailRequired(false);
        storedProvider.setSyncMappings(true);
        storedProvider.setGroupMappings(Map.of("true", new String[] { "group-1" }));
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedProvider));
        when(identityProviderRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIdentityProviderEntity update = new UpdateIdentityProviderEntity();
        update.setName("My OIDC");
        update.setDescription("kept");
        update.setEnabled(true);
        update.setEmailRequired(false);
        update.setSyncMappings(true);
        update.setConfiguration(new HashMap<>(Map.of("clientId", "client-id")));

        identityProviderService.update(GraviteeContext.getExecutionContext(), "idp-1", update);

        ArgumentCaptor<AuditService.AuditLogData> auditLog = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createOrganizationAuditLog(any(), auditLog.capture());
        IdentityProvider oldValue = (IdentityProvider) auditLog.getValue().getOldValue();
        IdentityProvider newValue = (IdentityProvider) auditLog.getValue().getNewValue();

        assertThat(oldValue).isNotSameAs(storedProvider);
        assertThat(oldValue.getConfiguration()).doesNotContainKey("clientSecret").containsEntry("clientId", "client-id");
        assertThat(oldValue.getDescription()).isEqualTo("kept");
        assertThat(oldValue.isEnabled()).isTrue();
        assertThat(oldValue.getEmailRequired()).isFalse();
        assertThat(oldValue.getSyncMappings()).isTrue();
        assertThat(oldValue.getGroupMappings()).containsKey("true");
        assertThat(newValue.getConfiguration()).doesNotContainKey("clientSecret");
        assertThat(storedProvider.getConfiguration()).containsEntry("clientSecret", STORED_SECRET);
    }

    @Test
    public void delete_should_omit_client_secret_from_audit_without_changing_the_stored_provider() throws Exception {
        IdentityProvider storedProvider = storedOidcProvider();
        storedProvider.setDescription("kept");
        when(identityProviderRepository.findById("idp-1")).thenReturn(Optional.of(storedProvider));

        identityProviderService.delete(GraviteeContext.getExecutionContext(), "idp-1");

        IdentityProvider audited = auditedProvider(false);
        assertThat(audited.getConfiguration()).doesNotContainKey("clientSecret");
        assertThat(audited.getDescription()).isEqualTo("kept");
        assertThat(audited.getId()).isEqualTo("idp-1");
        assertThat(storedProvider.getConfiguration()).containsEntry("clientSecret", STORED_SECRET);
    }

    private IdentityProvider auditedProvider(boolean newValue) {
        ArgumentCaptor<AuditService.AuditLogData> auditLog = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createOrganizationAuditLog(any(), auditLog.capture());
        return (IdentityProvider) (newValue ? auditLog.getValue().getNewValue() : auditLog.getValue().getOldValue());
    }

    private static IdentityProvider storedOidcProvider() {
        IdentityProvider idp = new IdentityProvider();
        idp.setId("idp-1");
        idp.setName("My OIDC");
        idp.setOrganizationId("DEFAULT");
        idp.setType(IdentityProviderType.OIDC);
        idp.setConfiguration(new HashMap<>(Map.of("clientId", "client-id", "clientSecret", STORED_SECRET)));
        return idp;
    }
}
