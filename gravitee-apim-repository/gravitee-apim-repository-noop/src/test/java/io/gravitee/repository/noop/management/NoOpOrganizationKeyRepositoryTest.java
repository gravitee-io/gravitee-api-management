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
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NoOpOrganizationKeyRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private OrganizationKeyRepository cut;

    @Test
    public void should_find_no_key() throws TechnicalException {
        assertThat(cut.findByOrganizationId("org-1")).isEmpty();
        assertThat(cut.findKekIds()).isEmpty();
    }

    @Test
    public void should_delete_without_error() {
        assertThatCode(() -> cut.deleteByOrganizationId("org-1")).doesNotThrowAnyException();
    }
}
