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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AmConnectionRepository;
import io.gravitee.repository.management.model.AmConnection;
import io.gravitee.rest.api.model.AmConnectionEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;

@RunWith(MockitoJUnitRunner.class)
public class AmConnectionServiceImplTest {

    @Mock
    private AmConnectionRepository amConnectionRepository;

    private DataEncryptor dataEncryptor;
    private AmConnectionServiceImpl service;

    @Before
    public void setUp() {
        dataEncryptor = new DataEncryptor(new MockEnvironment(), "test.secret", "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6");
        service = new AmConnectionServiceImpl(amConnectionRepository, dataEncryptor);
    }

    private static AmConnectionEntity entity(String baseUrl, String token) {
        AmConnectionEntity e = new AmConnectionEntity();
        e.setBaseUrl(baseUrl);
        e.setServiceAccountAccessToken(token);
        return e;
    }

    @Test
    public void save_with_non_blank_token_stores_ciphertext_and_returns_plaintext() throws Exception {
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.empty());
        when(amConnectionRepository.create(any())).thenAnswer(i -> i.getArgument(0));

        AmConnectionEntity result = service.save("org-1", entity("https://am.example.com", "my-token"));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(amConnectionRepository).create(captor.capture());
        AmConnection stored = captor.getValue();

        assertThat(stored.getServiceAccountAccessTokenEncrypted()).isNotNull().isNotEqualTo("my-token");
        assertThat(dataEncryptor.decrypt(stored.getServiceAccountAccessTokenEncrypted())).isEqualTo("my-token");
        assertThat(result.getServiceAccountAccessToken()).isEqualTo("my-token");
    }

    @Test
    public void save_with_null_token_preserves_existing_ciphertext() throws Exception {
        String existingCipher = dataEncryptor.encrypt("existing-token");
        AmConnection existing = new AmConnection();
        existing.setOrganizationId("org-1");
        existing.setBaseUrl("https://old.example.com");
        existing.setServiceAccountAccessTokenEncrypted(existingCipher);
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.of(existing));
        when(amConnectionRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        service.save("org-1", entity("https://am.example.com", null));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(amConnectionRepository).update(captor.capture());
        assertThat(captor.getValue().getServiceAccountAccessTokenEncrypted()).isEqualTo(existingCipher);
    }

    @Test
    public void save_with_blank_token_clears_ciphertext() throws Exception {
        AmConnection existing = new AmConnection();
        existing.setOrganizationId("org-1");
        existing.setServiceAccountAccessTokenEncrypted("existing-cipher");
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.of(existing));
        when(amConnectionRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        service.save("org-1", entity("https://am.example.com", "  "));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(amConnectionRepository).update(captor.capture());
        assertThat(captor.getValue().getServiceAccountAccessTokenEncrypted()).isNull();
    }

    @Test
    public void save_strips_trailing_slashes_and_sets_updated_at() throws Exception {
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.empty());
        when(amConnectionRepository.create(any())).thenAnswer(i -> i.getArgument(0));

        service.save("org-1", entity("https://am.example.com///", "tok"));

        ArgumentCaptor<AmConnection> captor = ArgumentCaptor.forClass(AmConnection.class);
        verify(amConnectionRepository).create(captor.capture());
        assertThat(captor.getValue().getBaseUrl()).isEqualTo("https://am.example.com");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        assertThat(captor.getValue().getOrganizationId()).isEqualTo("org-1");
    }

    @Test
    public void save_inserts_when_absent_updates_when_present() throws Exception {
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.empty());
        when(amConnectionRepository.create(any())).thenAnswer(i -> i.getArgument(0));
        service.save("org-1", entity("https://am.example.com", "tok"));
        verify(amConnectionRepository).create(any());
        verify(amConnectionRepository, never()).update(any());

        AmConnection existing = new AmConnection();
        existing.setOrganizationId("org-2");
        when(amConnectionRepository.findByOrganizationId("org-2")).thenReturn(Optional.of(existing));
        when(amConnectionRepository.update(any())).thenAnswer(i -> i.getArgument(0));
        service.save("org-2", entity("https://am.example.com", "tok"));
        verify(amConnectionRepository).update(any());
    }

    @Test
    public void find_by_organization_id_returns_decrypted_token() throws Exception {
        AmConnection stored = new AmConnection();
        stored.setOrganizationId("org-1");
        stored.setBaseUrl("https://am.example.com");
        stored.setServiceAccountAccessTokenEncrypted(dataEncryptor.encrypt("secret-token"));
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.of(stored));

        Optional<AmConnectionEntity> result = service.findByOrganizationId("org-1");

        assertThat(result).isPresent();
        assertThat(result.get().getServiceAccountAccessToken()).isEqualTo("secret-token");
        assertThat(result.get().getBaseUrl()).isEqualTo("https://am.example.com");
    }

    @Test
    public void save_throws_when_encryption_fails() throws Exception {
        DataEncryptor brokenEncryptor = Mockito.mock(DataEncryptor.class);
        when(brokenEncryptor.encrypt(any())).thenThrow(new GeneralSecurityException("key error"));
        AmConnectionServiceImpl broken = new AmConnectionServiceImpl(amConnectionRepository, brokenEncryptor);

        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> broken.save("org-1", entity("https://am.example.com", "tok")))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("encrypt");
    }

    @Test
    public void find_throws_when_decryption_fails() throws Exception {
        DataEncryptor brokenEncryptor = Mockito.mock(DataEncryptor.class);
        when(brokenEncryptor.decrypt(any())).thenThrow(new GeneralSecurityException("corrupted"));
        AmConnectionServiceImpl broken = new AmConnectionServiceImpl(amConnectionRepository, brokenEncryptor);

        AmConnection stored = new AmConnection();
        stored.setOrganizationId("org-1");
        stored.setServiceAccountAccessTokenEncrypted("some-cipher");
        when(amConnectionRepository.findByOrganizationId("org-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> broken.findByOrganizationId("org-1"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("decrypt");
    }

    @Test
    public void find_wraps_repository_exception() throws Exception {
        when(amConnectionRepository.findByOrganizationId("org-1")).thenThrow(new TechnicalException("DB down"));

        assertThatThrownBy(() -> service.findByOrganizationId("org-1"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("org-1");
    }

    @Test
    public void has_token_only_when_ciphertext_present() throws Exception {
        AmConnection withToken = new AmConnection();
        withToken.setServiceAccountAccessTokenEncrypted("cipher");
        when(amConnectionRepository.findByOrganizationId("with")).thenReturn(Optional.of(withToken));
        assertThat(service.hasToken("with")).isTrue();

        AmConnection withoutToken = new AmConnection();
        when(amConnectionRepository.findByOrganizationId("without")).thenReturn(Optional.of(withoutToken));
        assertThat(service.hasToken("without")).isFalse();

        when(amConnectionRepository.findByOrganizationId("absent")).thenReturn(Optional.empty());
        assertThat(service.hasToken("absent")).isFalse();
    }
}
