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
package io.gravitee.repository.noop.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NoOpSecretRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private SecretRepository cut;

    @Test
    public void should_find_no_secret() throws TechnicalException {
        assertThat(cut.findById("org-1", "env-1", "secret-1")).isEmpty();
        assertThat(cut.findByKey("org-1", "env-1", "anthropic-key")).isEmpty();
    }

    @Test
    public void should_have_no_secret_in_any_organization() throws TechnicalException {
        assertThat(cut.existsByOrganizationId("org-1")).isFalse();
    }

    @Test
    public void should_search_an_empty_page() throws TechnicalException {
        var page = cut.search("org-1", "env-1", new PageableBuilder().pageNumber(0).pageSize(10).build());

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    public void should_delete_without_error() {
        assertThatCode(() -> cut.delete("org-1", "env-1", "secret-1")).doesNotThrowAnyException();
        assertThatCode(() -> cut.deleteByEnvironmentId("env-1")).doesNotThrowAnyException();
    }
}
