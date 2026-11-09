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
import io.gravitee.repository.management.model.OrganizationKey;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class OrganizationKeyRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/organization-key-tests/";
    }

    @Test
    public void should_create_and_find_the_key_of_an_organization() throws Exception {
        var key = OrganizationKey.builder()
            .id("key-4")
            .organizationId("org-4")
            .wrappedKey("d3JhcHBlZC1rZXktNA==")
            .kekId("kek-a")
            .createdAt(new Date(1753711400000L))
            .build();

        organizationKeyRepository.create(key);

        assertThat(organizationKeyRepository.findByOrganizationId("org-4")).contains(key);
    }

    @Test
    public void should_find_no_key_for_an_organization_without_one() throws Exception {
        assertThat(organizationKeyRepository.findByOrganizationId("org-unknown")).isEmpty();
    }

    @Test
    public void should_refuse_a_second_key_for_one_organization() {
        var second = OrganizationKey.builder()
            .id("key-4")
            .organizationId("org-1")
            .wrappedKey("c2Vjb25k")
            .kekId("kek-a")
            .createdAt(new Date(1753711400000L))
            .build();

        assertThatThrownBy(() -> organizationKeyRepository.create(second))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("org-1")
            .hasMessageNotContaining("c2Vjb25k");
    }

    @Test
    public void should_list_the_key_encryption_key_ids_in_use() throws Exception {
        assertThat(organizationKeyRepository.findKekIds()).containsExactlyInAnyOrder("kek-a", "kek-b");
    }

    @Test
    public void should_delete_only_the_key_of_the_organization() throws Exception {
        organizationKeyRepository.deleteByOrganizationId("org-1");

        assertThat(organizationKeyRepository.findByOrganizationId("org-1")).isEmpty();
        assertThat(organizationKeyRepository.findByOrganizationId("org-2")).isPresent();
    }

    @Test
    public void should_not_hold_the_wrapped_key_in_to_string() throws Exception {
        var key = organizationKeyRepository.findByOrganizationId("org-1").orElseThrow();

        assertThat(key.toString()).contains("key-1").doesNotContain("d3JhcHBlZC1rZXktMQ==");
    }
}
