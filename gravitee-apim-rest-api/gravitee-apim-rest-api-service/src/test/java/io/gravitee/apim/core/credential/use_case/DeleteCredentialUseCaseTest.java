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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import io.gravitee.apim.core.credential.exception.CredentialInUseException;
import io.gravitee.apim.core.credential.exception.CredentialManagementNotAllowedException;
import io.gravitee.apim.core.credential.exception.CredentialNotFoundException;
import io.gravitee.apim.core.credential.exception.SecretPermissionRequiredException;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.credential.model.CredentialConsumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteCredentialUseCaseTest extends AbstractUseCaseTest {

    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CredentialFixture();
    }

    private void delete(CredentialActor actor, Credential credential) {
        fixture.deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(actor, credential.id()));
    }

    @Test
    void should_delete_a_credential_and_leave_the_secrets_it_references() {
        var credential = fixture.givenACredential();

        delete(fixture.owner(), credential);

        assertThat(fixture.credentialCrudService.storage()).isEmpty();
    }

    @Test
    void should_not_need_the_secret_delete_permission_for_a_credential_that_references_existing_secrets() {
        var credential = fixture.givenACredential();

        delete(fixture.ownerWithSecretAccess(), credential);

        assertThat(fixture.credentialCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_the_secrets_gravitee_stores() {
        var credential = fixture.givenAManagedCredential();

        delete(fixture.owner(), credential);

        assertThat(fixture.credentialCrudService.storage()).isEmpty();
        assertThat(fixture.secretStore.keys()).isEmpty();
    }

    @Test
    void should_delete_the_credential_when_its_secret_is_already_gone() {
        var credential = fixture.givenAManagedCredential();
        fixture.secretStore.removeSecret(credential.secretReferences().get("token").secretId());

        delete(fixture.owner(), credential);

        assertThat(fixture.credentialCrudService.storage()).isEmpty();
    }

    @Test
    void should_require_the_secret_delete_permission_for_the_secrets_gravitee_stores() {
        var credential = fixture.givenAManagedCredential();

        assertThatThrownBy(() -> delete(fixture.ownerWithSecretAccess(SecretAccess.CREATE, SecretAccess.UPDATE), credential)).isInstanceOf(
            SecretPermissionRequiredException.class
        );
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
        assertThat(fixture.secretStore.keys()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({ "org-id,env-2", "other-org,env-id" })
    void should_not_find_a_credential_of_another_organization_or_environment(String organizationId, String environmentId) {
        var credential = fixture.givenAManagedCredential();

        assertThatThrownBy(() -> delete(CredentialFixture.adminIn(organizationId, environmentId), credential)).isInstanceOf(
            CredentialNotFoundException.class
        );
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
        assertThat(fixture.secretStore.keys()).isNotEmpty();
    }

    @Test
    void should_not_let_another_user_delete_the_credential() {
        var credential = fixture.givenACredential();

        assertThatThrownBy(() -> delete(fixture.otherUser(), credential)).isInstanceOf(CredentialManagementNotAllowedException.class);
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
    }

    @Test
    void should_let_an_admin_delete_a_credential_they_do_not_own() {
        var credential = fixture.givenACredential();

        delete(fixture.admin(), credential);

        assertThat(fixture.credentialCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_to_delete_a_credential_in_use_and_name_the_consumers() {
        var credential = fixture.givenAManagedCredential();
        fixture.consumers.given(
            CredentialFixture.ENV_ID,
            credential.id(),
            new CredentialConsumer("aim", "LLM_PROXY", "proxy-1", "Support bot proxy")
        );
        fixture.consumers.given(
            CredentialFixture.ENV_ID,
            credential.id(),
            new CredentialConsumer("aim", "AI_WORKSPACE", "ws-1", "Research workspace")
        );

        assertThatThrownBy(() -> delete(fixture.owner(), credential))
            .isInstanceOf(CredentialInUseException.class)
            .hasMessageContaining("Support bot proxy")
            .hasMessageContaining("Research workspace");
        assertThat(fixture.credentialCrudService.storage()).containsExactly(credential);
        assertThat(fixture.secretStore.keys()).hasSize(1);
    }

    @Test
    void should_name_at_most_ten_consumers() {
        var credential = fixture.givenACredential();
        IntStream.rangeClosed(1, 12).forEach(i ->
            fixture.consumers.given(
                CredentialFixture.ENV_ID,
                credential.id(),
                new CredentialConsumer("aim", "LLM_PROXY", "proxy-" + i, "Proxy " + i)
            )
        );

        assertThatThrownBy(() -> delete(fixture.owner(), credential))
            .isInstanceOf(CredentialInUseException.class)
            .hasMessageContaining("Proxy 10")
            .hasMessageNotContaining("Proxy 11")
            .hasMessageContaining("2 more");
    }

    @Test
    void should_fail_when_the_credential_does_not_exist() {
        assertThatThrownBy(() ->
            fixture.deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(fixture.owner(), "missing"))
        ).isInstanceOf(CredentialNotFoundException.class);
    }

    @Test
    void should_audit_the_deletion() {
        var credential = fixture.givenACredential();
        fixture.auditCrudService.reset();

        delete(fixture.owner(), credential);

        assertThat(fixture.auditCrudService.storage())
            .singleElement()
            .satisfies(audit -> {
                assertThat(audit.getEvent()).isEqualTo("CREDENTIAL_DELETED");
                assertThat(audit.getProperties()).containsEntry("CREDENTIAL", credential.id());
            });
    }
}
