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
package io.gravitee.apim.core.credential.use_case;

import static io.gravitee.apim.core.credential.use_case.CredentialFixture.FOREIGN_USER_ID;
import static io.gravitee.apim.core.credential.use_case.CredentialFixture.NEXT_YEAR;
import static io.gravitee.apim.core.credential.use_case.CredentialFixture.OTHER_USER_ID;
import static io.gravitee.apim.core.credential.use_case.CredentialFixture.OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.credential.exception.SecretPermissionRequiredException;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.NewCredentialCommand;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.secret.model.SecretValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateCredentialUseCaseTest extends AbstractUseCaseTest {

    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CredentialFixture();
    }

    private CreateCredentialUseCase.Output create(String id, NewCredentialCommand command) {
        return fixture.createCredentialUseCase.execute(new CreateCredentialUseCase.Input(id, fixture.owner(), command));
    }

    @Nested
    class With_references_to_existing_secrets {

        @Test
        void should_store_the_references_as_typed() {
            // When
            var credential = create(
                "credential-1",
                NewCredentialCommand.builder()
                    .name("  OpenAI production  ")
                    .strategy(CredentialStrategy.BASIC)
                    .serviceId("openai")
                    .expiresAt(NEXT_YEAR)
                    .references(Map.of("username", "/vault/secret/openai:username", "password", "/vault/secret/openai:password"))
                    .build()
            ).credential();

            // Then
            assertThat(credential.id()).isEqualTo("credential-1");
            assertThat(credential.organizationId()).isEqualTo(CredentialFixture.ORG_ID);
            assertThat(credential.environmentId()).isEqualTo(CredentialFixture.ENV_ID);
            assertThat(credential.name()).isEqualTo("OpenAI production");
            assertThat(credential.serviceId()).isEqualTo("openai");
            assertThat(credential.expiresAt()).isEqualTo(NEXT_YEAR);
            assertThat(credential.createdAt()).isEqualTo(INSTANT_NOW);
            assertThat(credential.updatedAt()).isEqualTo(INSTANT_NOW);
            assertThat(credential.rotatedAt()).isNull();
            assertThat(credential.secretReferences()).containsOnly(
                Map.entry("username", new SecretReference("/vault/secret/openai:username", null)),
                Map.entry("password", new SecretReference("/vault/secret/openai:password", null))
            );
            assertThat(credential.isManaged()).isFalse();
            assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
            assertThat(fixture.secretStore.keys()).isEmpty();
        }

        @Test
        void should_make_the_actor_the_owner_when_no_owner_is_given() {
            var credential = fixture.givenACredential();

            assertThat(credential.ownerId()).isEqualTo(OWNER_ID);
        }

        @Test
        void should_accept_another_user_of_the_organization_as_owner() {
            var credential = fixture.givenACredential(c -> c.ownerId(OTHER_USER_ID));

            assertThat(credential.ownerId()).isEqualTo(OTHER_USER_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/gravitee/other-secret:value", "/GRAVITEE/my-secret:value" })
        void should_require_the_secret_read_permission_to_reference_a_secret_gravitee_stores(String reference) {
            var withoutRead = fixture.ownerWithSecretAccess(SecretAccess.CREATE, SecretAccess.UPDATE, SecretAccess.DELETE);
            var command = NewCredentialCommand.builder()
                .name("OpenAI")
                .strategy(CredentialStrategy.BEARER)
                .references(Map.of("token", reference))
                .build();

            assertThatThrownBy(() ->
                fixture.createCredentialUseCase.execute(new CreateCredentialUseCase.Input("credential-1", withoutRead, command))
            )
                .isInstanceOf(SecretPermissionRequiredException.class)
                .hasMessageContaining("read");
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_not_reference_the_secret_of_another_credential() {
            var command = NewCredentialCommand.builder()
                .name("OpenAI")
                .strategy(CredentialStrategy.BEARER)
                .references(Map.of("token", "/gravitee/credential.other.token:value"))
                .build();

            assertThatThrownBy(() ->
                fixture.createCredentialUseCase.execute(new CreateCredentialUseCase.Input("credential-1", fixture.admin(), command))
            )
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("another credential");
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_reference_a_secret_gravitee_stores_with_the_secret_read_permission() {
            var credential = fixture.createCredentialUseCase
                .execute(
                    new CreateCredentialUseCase.Input(
                        "credential-1",
                        fixture.ownerWithSecretAccess(SecretAccess.READ),
                        NewCredentialCommand.builder()
                            .name("OpenAI")
                            .strategy(CredentialStrategy.BEARER)
                            .references(Map.of("token", "/gravitee/my-secret:value"))
                            .build()
                    )
                )
                .credential();

            assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
        }

        @Test
        void should_not_need_any_secret_permission_to_reference_a_secret_outside_gravitee() {
            var credential = fixture.createCredentialUseCase
                .execute(
                    new CreateCredentialUseCase.Input(
                        "credential-1",
                        fixture.ownerWithSecretAccess(),
                        NewCredentialCommand.builder()
                            .name("OpenAI")
                            .strategy(CredentialStrategy.BEARER)
                            .references(Map.of("token", "/vault/secret/openai:token"))
                            .build()
                    )
                )
                .credential();

            assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
        }

        @Test
        void should_reject_a_reference_that_is_not_an_absolute_path() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.references(Map.of("token", "vault/secret/openai:token"))))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("token");
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_reject_a_reference_that_holds_an_expression() {
            assertThatThrownBy(() ->
                fixture.givenACredential(c -> c.references(Map.of("token", "/vault/{#context.attributes['x']}")))
            ).isInstanceOf(InvalidCredentialException.class);
        }
    }

    @Nested
    class With_values_gravitee_stores {

        @Test
        void should_write_each_value_to_the_secret_store_and_keep_only_the_references() {
            // When
            var credential = create(
                "credential-1",
                NewCredentialCommand.builder()
                    .name("Azure OpenAI")
                    .strategy(CredentialStrategy.OAUTH2)
                    .configuration(Map.of("tokenUrl", "https://login.example.com/oauth2/token", "scopes", List.of("read")))
                    .values(Map.of("clientId", "my-client", "clientSecret", "my-secret"))
                    .build()
            ).credential();

            // Then
            assertThat(fixture.secretStore.valueOf("credential.credential-1.clientId")).contains("my-client");
            assertThat(fixture.secretStore.valueOf("credential.credential-1.clientSecret")).contains("my-secret");
            assertThat(credential.secretReferences()).containsOnly(
                Map.entry(
                    "clientId",
                    new SecretReference("/gravitee/credential.credential-1.clientId:value?renewable=true&reloadOnChange=true", "secret-1")
                ),
                Map.entry(
                    "clientSecret",
                    new SecretReference(
                        "/gravitee/credential.credential-1.clientSecret:value?renewable=true&reloadOnChange=true",
                        "secret-2"
                    )
                )
            );
            assertThat(credential.isManaged()).isTrue();
            assertThat(credential.toString()).doesNotContain("my-secret");
        }

        @Test
        void should_require_the_secret_create_permission() {
            assertThatThrownBy(() ->
                fixture.createCredentialUseCase.execute(
                    new CreateCredentialUseCase.Input(
                        "credential-1",
                        fixture.ownerWithSecretAccess(SecretAccess.UPDATE, SecretAccess.DELETE),
                        NewCredentialCommand.builder()
                            .name("Anthropic")
                            .strategy(CredentialStrategy.BEARER)
                            .values(Map.of("token", "sk-1"))
                            .build()
                    )
                )
            ).isInstanceOf(SecretPermissionRequiredException.class);
            assertThat(fixture.secretStore.keys()).isEmpty();
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_reject_a_blank_value() {
            assertThatThrownBy(() -> fixture.givenAManagedCredential(c -> c.values(Map.of("token", "  ")))).isInstanceOf(
                InvalidCredentialException.class
            );
            assertThat(fixture.secretStore.keys()).isEmpty();
        }

        @Test
        void should_reject_a_value_larger_than_a_secret_holds_before_writing_any_secret() {
            assertThatThrownBy(() ->
                fixture.givenAManagedCredential(c ->
                    c
                        .strategy(CredentialStrategy.BASIC)
                        .values(Map.of("username", "bob", "password", "p".repeat(SecretValue.MAX_BYTES + 1)))
                )
            )
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("password");
            assertThat(fixture.secretStore.keys()).isEmpty();
        }

        @Test
        void should_delete_the_secrets_already_written_when_a_later_secret_write_fails() {
            fixture.secretStore.failOn("password");

            assertThatThrownBy(() ->
                fixture.givenAManagedCredential(c ->
                    c.strategy(CredentialStrategy.BASIC).values(Map.of("username", "bob", "password", "s3cret"))
                )
            ).isInstanceOf(IllegalStateException.class);

            assertThat(fixture.secretStore.keys()).isEmpty();
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_keep_the_write_failure_and_attach_the_failed_cleanup_when_a_secret_cannot_be_deleted() {
            fixture.secretStore.failOn("password");
            fixture.secretStore.failDeleteOf("username");

            assertThatThrownBy(() ->
                fixture.givenAManagedCredential(c ->
                    c.strategy(CredentialStrategy.BASIC).values(Map.of("username", "bob", "password", "pa55"))
                )
            )
                .hasMessageContaining("password")
                .satisfies(e ->
                    assertThat(e.getSuppressed())
                        .singleElement()
                        .satisfies(s -> assertThat(s).hasMessageContaining("username"))
                );
            assertThat(fixture.credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_delete_the_secrets_when_the_credential_cannot_be_saved() {
            fixture.credentialCrudService.failOnCreate();

            assertThatThrownBy(() -> fixture.givenAManagedCredential()).isInstanceOf(IllegalStateException.class);

            assertThat(fixture.secretStore.keys()).isEmpty();
        }

        @Test
        void should_not_print_the_values_of_the_command() {
            var command = NewCredentialCommand.builder()
                .name("Anthropic")
                .strategy(CredentialStrategy.BEARER)
                .values(Map.of("token", "sk-very-secret"))
                .build();

            assertThat(command.toString()).doesNotContain("sk-very-secret");
        }
    }

    @Nested
    class Validation {

        @Test
        void should_reject_a_command_with_both_references_and_values() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.values(Map.of("token", "sk-1"))))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("either");
        }

        @Test
        void should_reject_a_command_with_neither_references_nor_values() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.references(null))).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_a_missing_secret_field() {
            assertThatThrownBy(() ->
                fixture.givenACredential(c -> c.strategy(CredentialStrategy.BASIC).references(Map.of("username", "/vault/u")))
            )
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining("password");
        }

        @Test
        void should_reject_a_secret_field_the_strategy_does_not_have() {
            assertThatThrownBy(() ->
                fixture.givenACredential(c -> c.references(Map.of("token", "/vault/t", "password", "/vault/p")))
            ).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_a_missing_strategy() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.strategy(null))).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_a_blank_name() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.name("  "))).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_a_name_longer_than_256_characters() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.name("n".repeat(257)))).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_a_service_longer_than_256_characters() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.serviceId("s".repeat(257)))).isInstanceOf(
                InvalidCredentialException.class
            );
        }

        @Test
        void should_reject_an_expiration_in_the_past() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.expiresAt(Instant.parse("2023-01-01T00:00:00Z")))).isInstanceOf(
                InvalidCredentialException.class
            );
        }

        @Test
        void should_reject_an_owner_from_another_organization() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.ownerId(FOREIGN_USER_ID)))
                .isInstanceOf(InvalidCredentialException.class)
                .hasMessageContaining(FOREIGN_USER_ID);
        }

        @Test
        void should_reject_an_unknown_owner() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.ownerId("nobody"))).isInstanceOf(InvalidCredentialException.class);
        }

        @Test
        void should_reject_settings_the_strategy_does_not_accept() {
            assertThatThrownBy(() -> fixture.givenACredential(c -> c.configuration(Map.of("headerName", "X-Api-Key")))).isInstanceOf(
                InvalidCredentialException.class
            );
        }

        @Test
        void should_validate_before_writing_any_secret() {
            assertThatThrownBy(() -> fixture.givenAManagedCredential(c -> c.name(""))).isInstanceOf(InvalidCredentialException.class);

            assertThat(fixture.secretStore.keys()).isEmpty();
        }
    }

    @Test
    void should_audit_the_creation() {
        var credential = fixture.givenACredential();

        assertThat(fixture.auditCrudService.storage())
            .singleElement()
            .satisfies(audit -> {
                assertThat(audit.getEvent()).isEqualTo("CREDENTIAL_CREATED");
                assertThat(audit.getReferenceType()).isEqualTo(AuditEntity.AuditReferenceType.ENVIRONMENT);
                assertThat(audit.getReferenceId()).isEqualTo(CredentialFixture.ENV_ID);
                assertThat(audit.getOrganizationId()).isEqualTo(CredentialFixture.ORG_ID);
                assertThat(audit.getUser()).isEqualTo(OWNER_ID);
                assertThat(audit.getProperties()).containsEntry("CREDENTIAL", credential.id());
            });
    }
}
