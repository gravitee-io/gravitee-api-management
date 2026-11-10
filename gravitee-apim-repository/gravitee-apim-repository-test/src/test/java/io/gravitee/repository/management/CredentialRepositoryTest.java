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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Credential;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class CredentialRepositoryTest extends AbstractManagementRepositoryTest {

    private static final Pageable FIRST_PAGE = new PageableBuilder().pageNumber(0).pageSize(10).build();

    @Override
    protected String getTestCasesPath() {
        return "/data/credential-tests/";
    }

    @Test
    public void should_create_and_find_a_credential_with_every_field() throws Exception {
        var credential = aCredential("new-credential").build();

        credentialRepository.create(credential);

        assertThat(credentialRepository.findById("org-1", "env-1", "new-credential")).contains(credential);
    }

    @Test
    public void should_create_a_credential_without_its_optional_fields() throws Exception {
        var credential = aCredential("minimal").serviceId(null).ownerId(null).expiresAt(null).rotatedAt(null).build();

        credentialRepository.create(credential);

        assertThat(credentialRepository.findById("org-1", "env-1", "minimal")).contains(credential);
    }

    @Test
    public void should_keep_secret_references_of_several_kilobytes() throws Exception {
        var references = "{\"serviceAccountKey\":{\"reference\":\"/vault/" + "a".repeat(8 * 1024) + ":key\"}}";
        var credential = aCredential("large").secretReferences(references).build();

        credentialRepository.create(credential);

        assertThat(credentialRepository.findById("org-1", "env-1", "large")).map(Credential::getSecretReferences).contains(references);
    }

    @Test
    public void should_refuse_a_second_credential_with_the_same_id() {
        var sameId = aCredential("credential-1").name("Another").build();

        assertThatThrownBy(() -> credentialRepository.create(sameId))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("credential-1");
    }

    @Test
    public void should_accept_two_credentials_with_the_same_name_in_one_environment() throws Exception {
        credentialRepository.create(aCredential("same-name").name("Anthropic production").build());

        assertThat(credentialRepository.search("org-1", "env-1", "anthropic production", FIRST_PAGE).getContent())
            .extracting(Credential::getId)
            .containsExactlyInAnyOrder("credential-1", "same-name");
    }

    @Test
    public void should_not_find_a_credential_from_another_environment_or_organization() throws Exception {
        assertThat(credentialRepository.findById("org-1", "env-2", "credential-1")).isEmpty();
        assertThat(credentialRepository.findById("org-2", "env-1", "credential-1")).isEmpty();
    }

    @Test
    public void should_change_everything_but_the_scope_the_strategy_and_the_creation_date() throws Exception {
        var existing = credentialRepository.findById("org-1", "env-1", "credential-1").orElseThrow();
        var changed = existing
            .toBuilder()
            .name("Anthropic renamed")
            .serviceId("anthropic-eu")
            .ownerId("user-2")
            .expiresAt(new Date(1790000000000L))
            .configuration("{\"headerName\":\"authorization\"}")
            .secretReferences("{\"value\":{\"reference\":\"/vault/anthropic:key\"}}")
            .updatedAt(new Date(1753711900000L))
            .rotatedAt(new Date(1753711900000L))
            .build();

        credentialRepository.update(changed.toBuilder().strategy("BEARER").createdAt(new Date(1700000000000L)).build());

        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).contains(changed);
    }

    @Test
    public void should_clear_the_optional_fields_on_update() throws Exception {
        var existing = credentialRepository.findById("org-1", "env-1", "credential-1").orElseThrow();
        var cleared = existing.toBuilder().serviceId(null).ownerId(null).expiresAt(null).build();

        credentialRepository.update(cleared);

        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).contains(cleared);
    }

    @Test
    public void should_update_a_credential_whose_id_looks_like_an_object_id() throws Exception {
        var created = credentialRepository.create(aCredential("5f8d0d55b54764421b7156c9").build());
        var renamed = created.toBuilder().name("Renamed").updatedAt(new Date(1753711900000L)).build();

        credentialRepository.update(renamed);

        assertThat(credentialRepository.findById("org-1", "env-1", "5f8d0d55b54764421b7156c9")).contains(renamed);
    }

    @Test
    public void should_refuse_to_update_a_credential_of_another_environment() throws Exception {
        var original = credentialRepository.findById("org-1", "env-1", "credential-1").orElseThrow();

        assertThatThrownBy(() ->
            credentialRepository.update(original.toBuilder().environmentId("env-2").name("Hijacked").build())
        ).isInstanceOf(TechnicalException.class);
        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).contains(original);
    }

    @Test
    public void should_refuse_to_update_a_credential_of_another_organization() throws Exception {
        var original = credentialRepository.findById("org-1", "env-1", "credential-1").orElseThrow();

        assertThatThrownBy(() ->
            credentialRepository.update(original.toBuilder().organizationId("org-2").name("Hijacked").build())
        ).isInstanceOf(TechnicalException.class);
        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).contains(original);
    }

    @Test
    public void should_refuse_to_update_a_deleted_credential_rather_than_recreate_it() throws Exception {
        var existing = credentialRepository.findById("org-1", "env-1", "credential-1").orElseThrow();
        credentialRepository.delete("org-1", "env-1", "credential-1");

        assertThatThrownBy(() -> credentialRepository.update(existing.toBuilder().name("Late").build())).isInstanceOf(
            TechnicalException.class
        );
        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).isEmpty();
    }

    @Test
    public void should_delete_a_credential() throws Exception {
        credentialRepository.delete("org-1", "env-1", "credential-1");

        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).isEmpty();
    }

    @Test
    public void should_not_delete_a_credential_of_another_environment_or_organization() throws Exception {
        credentialRepository.delete("org-1", "env-2", "credential-1");
        credentialRepository.delete("org-2", "env-1", "credential-1");

        assertThat(credentialRepository.findById("org-1", "env-1", "credential-1")).isPresent();
    }

    @Test
    public void should_page_the_credentials_of_one_environment_sorted_by_name() throws Exception {
        var firstPage = credentialRepository.search("org-1", "env-1", null, new PageableBuilder().pageNumber(0).pageSize(2).build());
        var secondPage = credentialRepository.search("org-1", "env-1", null, new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertThat(firstPage.getContent()).extracting(Credential::getId).containsExactly("credential-1", "credential-3");
        assertThat(firstPage.getPageNumber()).isZero();
        assertThat(firstPage.getPageElements()).isEqualTo(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).extracting(Credential::getId).containsExactly("credential-2");
        assertThat(secondPage.getPageNumber()).isEqualTo(1);
        assertThat(secondPage.getPageElements()).isEqualTo(1);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void should_match_the_query_anywhere_in_the_name_ignoring_case() throws Exception {
        var page = credentialRepository.search("org-1", "env-1", "STAG", FIRST_PAGE);

        assertThat(page.getContent()).extracting(Credential::getId).containsExactly("credential-2");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void should_match_the_query_as_literal_text() throws Exception {
        assertThat(credentialRepository.search("org-1", "env-1", "100%_", FIRST_PAGE).getContent())
            .extracting(Credential::getId)
            .containsExactly("credential-3");
        assertThat(credentialRepository.search("org-1", "env-1", "%", FIRST_PAGE).getContent())
            .extracting(Credential::getId)
            .containsExactly("credential-3");
        assertThat(credentialRepository.search("org-1", "env-1", "_", FIRST_PAGE).getContent())
            .extracting(Credential::getId)
            .containsExactly("credential-3");
        assertThat(credentialRepository.search("org-1", "env-1", ".*", FIRST_PAGE).getContent()).isEmpty();
    }

    @Test
    public void should_return_an_empty_page_past_the_last_one() throws Exception {
        var page = credentialRepository.search("org-1", "env-1", null, new PageableBuilder().pageNumber(5).pageSize(2).build());

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getPageNumber()).isEqualTo(5);
        assertThat(page.getPageElements()).isZero();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void should_return_an_empty_page_for_another_organization() throws Exception {
        var page = credentialRepository.search("org-2", "env-1", null, FIRST_PAGE);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    public void should_delete_every_credential_of_one_environment_only() throws Exception {
        credentialRepository.deleteByEnvironmentId("env-1");

        assertThat(credentialRepository.search("org-1", "env-1", null, FIRST_PAGE).getContent()).isEmpty();
        assertThat(credentialRepository.search("org-1", "env-2", null, FIRST_PAGE).getContent())
            .extracting(Credential::getId)
            .containsExactly("credential-4");
    }

    private static Credential.CredentialBuilder aCredential(String id) {
        return Credential.builder()
            .id(id)
            .organizationId("org-1")
            .environmentId("env-1")
            .name("Credential " + id)
            .strategy("OAUTH2")
            .serviceId("openai")
            .ownerId("user-1")
            .expiresAt(new Date(1785247200000L))
            .configuration("{\"tokenUrl\":\"https://auth.example.com/token\",\"scopes\":[\"read\"]}")
            .secretReferences(
                "{\"clientId\":{\"reference\":\"/gravitee/credential." +
                    id +
                    ".clientId:value\",\"secretId\":\"s1\"},\"clientSecret\":{\"reference\":\"/gravitee/credential." +
                    id +
                    ".clientSecret:value\",\"secretId\":\"s2\"}}"
            )
            .createdAt(new Date(1753711200000L))
            .updatedAt(new Date(1753711300000L))
            .rotatedAt(new Date(1753711300000L));
    }
}
