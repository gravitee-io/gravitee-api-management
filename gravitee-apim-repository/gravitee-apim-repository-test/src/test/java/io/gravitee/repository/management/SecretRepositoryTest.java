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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Secret;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class SecretRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/secret-tests/";
    }

    @Test
    public void should_create_and_find_a_secret_by_id() throws Exception {
        var secret = Secret.builder()
            .id("new-secret")
            .organizationId("org-1")
            .environmentId("env-1")
            .key("openai-key")
            .encryptedValue("ciphertext-new")
            .createdAt(new Date(1753711200000L))
            .updatedAt(new Date(1753711300000L))
            .build();

        secretRepository.create(secret);

        assertThat(secretRepository.findById("org-1", "env-1", "new-secret")).contains(secret);
    }

    @Test
    public void should_not_find_a_secret_from_another_environment_or_organization() throws Exception {
        assertThat(secretRepository.findById("org-1", "env-2", "secret-1")).isEmpty();
        assertThat(secretRepository.findById("org-2", "env-1", "secret-1")).isEmpty();
    }

    @Test
    public void should_refuse_a_second_secret_with_the_same_key_in_one_environment() {
        var duplicate = aSecret("duplicate").key("anthropic-key").build();

        assertThatThrownBy(() -> secretRepository.create(duplicate)).isInstanceOf(TechnicalException.class);
    }

    @Test
    public void should_accept_the_same_key_in_another_environment() throws Exception {
        var sameKeyOtherEnv = aSecret("other-env").environmentId("env-3").key("anthropic-key").build();

        secretRepository.create(sameKeyOtherEnv);

        assertThat(secretRepository.findById("org-1", "env-3", "other-env")).contains(sameKeyOtherEnv);
    }

    @Test
    public void should_find_a_secret_by_key_in_its_environment_only() throws Exception {
        assertThat(secretRepository.findByKey("org-1", "env-1", "anthropic-key")).map(Secret::getId).contains("secret-1");
        assertThat(secretRepository.findByKey("org-1", "env-3", "anthropic-key")).isEmpty();
        assertThat(secretRepository.findByKey("org-2", "env-1", "anthropic-key")).isEmpty();
    }

    @Test
    public void should_rotate_the_value_of_a_secret() throws Exception {
        var existing = secretRepository.findById("org-1", "env-1", "secret-1").orElseThrow();
        var rotated = existing.toBuilder().encryptedValue("ciphertext-rotated").updatedAt(new Date(1753711900000L)).build();

        secretRepository.update(rotated);

        assertThat(secretRepository.findById("org-1", "env-1", "secret-1")).contains(rotated);
    }

    @Test
    public void should_refuse_to_update_a_secret_of_another_environment() {
        var wrongEnv = aSecret("secret-1").environmentId("env-2").build();

        assertThatThrownBy(() -> secretRepository.update(wrongEnv)).isInstanceOf(TechnicalException.class);
    }

    @Test
    public void should_delete_a_secret_so_it_no_longer_resolves() throws Exception {
        secretRepository.delete("org-1", "env-1", "secret-1");

        assertThat(secretRepository.findById("org-1", "env-1", "secret-1")).isEmpty();
        assertThat(secretRepository.findByKey("org-1", "env-1", "anthropic-key")).isEmpty();
    }

    @Test
    public void should_not_delete_a_secret_of_another_environment() throws Exception {
        secretRepository.delete("org-1", "env-2", "secret-1");

        assertThat(secretRepository.findById("org-1", "env-1", "secret-1")).isPresent();
    }

    @Test
    public void should_page_the_secrets_of_one_environment_sorted_by_key() throws Exception {
        var firstPage = secretRepository.search("org-1", "env-1", new PageableBuilder().pageNumber(0).pageSize(2).build());
        var secondPage = secretRepository.search("org-1", "env-1", new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertThat(firstPage.getContent()).extracting(Secret::getKey).containsExactly("anthropic-key", "cohere-key");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).extracting(Secret::getKey).containsExactly("mistral-key");
    }

    @Test
    public void should_return_an_empty_page_for_another_organization() throws Exception {
        var page = secretRepository.search("org-2", "env-1", new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    public void should_delete_every_secret_of_one_environment_only() throws Exception {
        secretRepository.deleteByEnvironmentId("env-1");

        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        assertThat(secretRepository.search("org-1", "env-1", pageable).getContent()).isEmpty();
        assertThat(secretRepository.search("org-1", "env-2", pageable).getContent()).extracting(Secret::getId).containsExactly("secret-4");
    }

    private static Secret.SecretBuilder aSecret(String id) {
        return Secret.builder()
            .id(id)
            .organizationId("org-1")
            .environmentId("env-1")
            .key(id + "-key")
            .encryptedValue("ciphertext-" + id)
            .createdAt(new Date(1753711200000L))
            .updatedAt(new Date(1753711300000L));
    }
}
