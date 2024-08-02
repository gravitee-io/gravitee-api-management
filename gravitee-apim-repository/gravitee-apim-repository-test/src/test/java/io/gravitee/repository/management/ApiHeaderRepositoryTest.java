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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.ApiHeader;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiHeaderRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/apiheader-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<ApiHeader> all = apiHeaderRepository.findAll();
        assertEquals(5, all.size());
    }

    @Test
    public void shouldFindAllByEnvironment() throws Exception {
        Set<ApiHeader> all = apiHeaderRepository.findAllByEnvironment("DEFAULT");
        assertEquals(2, all.size());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<ApiHeader> optionalApiHeader = apiHeaderRepository.findById("1");
        assertTrue(optionalApiHeader.isPresent());

        apiHeaderRepository.delete("1");

        optionalApiHeader = apiHeaderRepository.findById("1");
        assertFalse(optionalApiHeader.isPresent());
    }

    @Test
    public void shouldUpdate() throws Exception {
        ApiHeader up = new ApiHeader();
        up.setId("toUpdate");
        up.setEnvironmentId("new_DEFAULT");
        up.setName("newName");
        up.setValue("newValue");
        up.setOrder(123);
        up.setCreatedAt(new Date(1439027010882L));
        up.setUpdatedAt(new Date(1439027010883L));

        apiHeaderRepository.update(up);

        Optional<ApiHeader> updated = apiHeaderRepository.findById("toUpdate");

        assertTrue(updated.isPresent());
        assertEquals(up.getId(), updated.get().getId());
        assertEquals(up.getEnvironmentId(), updated.get().getEnvironmentId());
        assertEquals(up.getName(), updated.get().getName());
        assertEquals(up.getValue(), updated.get().getValue());
        assertEquals(up.getOrder(), updated.get().getOrder());
        assertTrue(compareDate(up.getCreatedAt(), updated.get().getCreatedAt()));
        assertTrue(compareDate(up.getUpdatedAt(), updated.get().getUpdatedAt()));
    }

    @Test
    public void should_delete_by_environment_id() throws Exception {
        final var beforeDeletion = apiHeaderRepository.findAllByEnvironment("env_id_to_be_deleted");

        final var deleted = apiHeaderRepository.deleteByEnvironmentId("env_id_to_be_deleted");
        final var nbAfterDeletion = apiHeaderRepository.findAllByEnvironment("env_id_to_be_deleted").size();

        assertThat(beforeDeletion.size()).isEqualTo(deleted.size());
        assertThat(deleted).containsOnly("3", "4");
        assertThat(nbAfterDeletion).isEqualTo(0);
    }
}
