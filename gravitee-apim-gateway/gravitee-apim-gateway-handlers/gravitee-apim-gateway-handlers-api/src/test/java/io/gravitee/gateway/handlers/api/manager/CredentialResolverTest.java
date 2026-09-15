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
package io.gravitee.gateway.handlers.api.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.secrets.api.el.FieldKind;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialResolverTest {

    private static final SecretFieldAccessControl SECRET_FIELD = new SecretFieldAccessControl(true, FieldKind.PASSWORD, "clientSecret");
    private static final SecretFieldAccessControl PLAIN_FIELD = new SecretFieldAccessControl(false, null, null);

    private final DataEncryptor dataEncryptor = new DataEncryptor(
        new MockEnvironment(),
        "api.properties.encryption.secret",
        "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6"
    );

    @Mock
    private CredentialManager credentialManager;

    private CredentialResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CredentialResolver(credentialManager, dataEncryptor, new ObjectMapper());
    }

    @Nested
    class AccessTest {

        @Test
        void should_resolve_a_field_inside_a_secret_field() throws Exception {
            deploy("{\"clientId\": \"my-client\", \"clientSecret\": \"s3cr3t\"}");

            assertThat(resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD)).isEqualTo("s3cr3t");
        }

        @Test
        void should_refuse_outside_a_secret_field() {
            assertRefused(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", PLAIN_FIELD));
        }

        @Test
        void should_refuse_when_no_field_is_being_evaluated() {
            assertRefused(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", null));
        }

        private void assertRefused(ThrowingCallable resolution) {
            assertThatThrownBy(resolution)
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Credential [credential-1] can only be resolved in a secret field");
            verifyNoInteractions(credentialManager);
        }
    }

    @Nested
    class AllowedApiTest {

        @Test
        void should_refuse_an_api_the_credential_is_not_allowed_for() throws Exception {
            DataEncryptor spiedEncryptor = spy(dataEncryptor);
            resolver = new CredentialResolver(credentialManager, spiedEncryptor, new ObjectMapper());
            deploy("{\"clientSecret\": \"s3cr3t\"}");

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-2", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Credential [credential-1] is not allowed for API [api-2]");
            verify(spiedEncryptor, never()).decrypt(anyString());
        }

        @Test
        void should_refuse_every_api_when_no_api_is_allowed() throws Exception {
            when(credentialManager.get("env-1", "credential-1")).thenReturn(
                Optional.of(
                    new DeployedCredential(
                        "credential-1",
                        "env-1",
                        "org-1",
                        null,
                        dataEncryptor.encrypt("{\"clientSecret\": \"s3cr3t\"}"),
                        1L
                    )
                )
            );

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Credential [credential-1] is not allowed for API [api-1]");
        }

        @Test
        void should_refuse_when_the_api_is_unknown() throws Exception {
            deploy("{\"clientSecret\": \"s3cr3t\"}");

            assertThatThrownBy(() -> resolver.resolve("env-1", null, "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Credential [credential-1] is not allowed for API [null]");
        }
    }

    @Nested
    class LookupTest {

        @Test
        void should_fail_for_a_credential_not_deployed_in_the_environment() {
            when(credentialManager.get("env-2", "credential-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolve("env-2", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Credential [credential-1] not found in environment [env-2]");
        }

        @Test
        void should_fail_for_an_unknown_field() throws Exception {
            deploy("{\"token\": \"s3cr3t\"}");

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Field [clientSecret] not found in credential [credential-1]");
        }

        @Test
        void should_fail_for_a_field_that_is_not_text() throws Exception {
            deploy("{\"scopes\": [\"read\", \"write\"]}");

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "scopes", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Field [scopes] not found in credential [credential-1]");
        }
    }

    @Nested
    class DecryptionTest {

        @Test
        void should_fail_when_the_secret_cannot_be_decrypted() {
            when(credentialManager.get("env-1", "credential-1")).thenReturn(Optional.of(credential("not-a-ciphertext")));

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Unable to decrypt credential [credential-1]");
        }

        @Test
        void should_not_reveal_a_decrypted_value_that_is_not_json() throws Exception {
            deploy("s3cr3t-not-json");

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Unable to read credential [credential-1]")
                .hasNoCause();
        }

        @Test
        void should_decrypt_again_on_every_resolution() throws Exception {
            DataEncryptor spiedEncryptor = spy(dataEncryptor);
            resolver = new CredentialResolver(credentialManager, spiedEncryptor, new ObjectMapper());
            deploy("{\"clientSecret\": \"s3cr3t\"}");

            String first = resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD);
            String second = resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD);

            assertThat(first).isEqualTo("s3cr3t");
            assertThat(second).isEqualTo("s3cr3t");
            verify(spiedEncryptor, times(2)).decrypt(anyString());
        }

        @Test
        void should_fail_when_the_decrypted_value_is_null() throws Exception {
            deploy("null");

            assertThatThrownBy(() -> resolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD))
                .isInstanceOf(CredentialResolutionException.class)
                .hasMessage("Unable to read credential [credential-1]");
        }
    }

    private void deploy(String plaintext) throws Exception {
        when(credentialManager.get("env-1", "credential-1")).thenReturn(Optional.of(credential(dataEncryptor.encrypt(plaintext))));
    }

    private static DeployedCredential credential(String encryptedSecret) {
        return new DeployedCredential("credential-1", "env-1", "org-1", Set.of("api-1"), encryptedSecret, 1L);
    }
}
