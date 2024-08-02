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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Integration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class IntegrationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/integration-tests/";
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        Integration integration = creatIntegration(uuid, date);

        Integration createdIntegration = integrationRepository.create(integration);

        assertThat(createdIntegration).isEqualTo(integration);
    }

    @Test
    public void shouldThrowExceptionWhenInsertSameIdIntegration() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        Integration integration = creatIntegration(uuid, date);

        integrationRepository.create(integration);
        assertThatThrownBy(() -> integrationRepository.create(integration)).isInstanceOf(Exception.class);
    }

    private static Integration creatIntegration(String uuid, Date date) {
        return Integration
            .builder()
            .id(uuid)
            .name("my-another-integration")
            .description("test-description")
            .provider("sample-provider")
            .environmentId("my-env")
            .agentStatus(Integration.AgentStatus.DISCONNECTED)
            .createdAt(date)
            .updatedAt(date)
            .build();
    }

    @Test
    public void shouldFindByEnvironmentId() throws TechnicalException {
        final List<Integration> integrations = integrationRepository
            .findAllByEnvironment("my-env", new PageableBuilder().pageSize(10).pageNumber(0).build())
            .getContent();

        assertThat(integrations)
            .hasSize(3)
            .extracting(Integration::getId, Integration::getName, Integration::getDescription, Integration::getEnvironmentId)
            .contains(
                tuple("cad107c9-27f2-40b2-9107-c927f2e0b2fc", "my-integration", "test-description", "my-env"),
                tuple("f66274c9-3d8f-44c5-a274-c93d8fb4c5f3", "my-another-integration", "test-description", "my-env"),
                tuple("459a022c-e79c-4411-9a02-2ce79c141165", "my-yet-another-integration", "test-description", "my-env")
            );
    }

    @Test
    public void shouldReturnEmptyListWhenEnvironmentIdNotFound() throws TechnicalException {
        final List<Integration> integrations = integrationRepository
            .findAllByEnvironment("other-env", new PageableBuilder().pageSize(10).pageNumber(0).build())
            .getContent();

        assertThat(integrations).hasSize(0);
    }

    @Test
    public void should_get_integration_by_id() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";
        var date = new Date(1470157767000L);
        var expectedIntegration = creatIntegration(id, date);

        final Optional<Integration> integration = integrationRepository.findById(id);

        assertThat(integration).isPresent().isNotEmpty().hasValue(expectedIntegration);
    }

    @Test
    public void should_return_empty_when_integration_not_found() throws TechnicalException {
        var id = "not-existing-id";

        final Optional<Integration> integration = integrationRepository.findById(id);

        assertThat(integration).isNotPresent();
    }

    @Test
    public void should_update_integration() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";
        var date = new Date(1470157767000L);
        var updateDate = new Date(1712660289);
        Integration integration = Integration
            .builder()
            .id(id)
            .name("my-updated-integration")
            .description("updated-description")
            .provider("sample-provider")
            .environmentId("my-env")
            .agentStatus(Integration.AgentStatus.DISCONNECTED)
            .createdAt(date)
            .updatedAt(updateDate)
            .build();

        final Integration updatedIntegration = integrationRepository.update(integration);
        assertThat(updatedIntegration).isEqualTo(integration);
    }

    @Test
    public void should_throw_exception_when_integration_to_update_not_found() {
        var id = "not-existing-id";
        var date = new Date(1470157767000L);
        Integration integration = creatIntegration(id, date);

        assertThatThrownBy(() -> integrationRepository.update(integration)).isInstanceOf(Exception.class);
    }

    @Test
    public void should_delete_integration() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";

        integrationRepository.delete(id);

        var deletedIntegration = integrationRepository.findById(id);

        assertThat(deletedIntegration).isEmpty();
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        long nbBeforeDeletion = integrationRepository
            .findAllByEnvironment("ToBeDeleted", new PageableBuilder().pageSize(10).pageNumber(0).build())
            .getTotalElements();
        int deleted = integrationRepository.deleteByEnvironmentId("ToBeDeleted").size();
        long nbAfterDeletion = integrationRepository
            .findAllByEnvironment("ToBeDeleted", new PageableBuilder().pageSize(10).pageNumber(0).build())
            .getTotalElements();

        assertThat(nbBeforeDeletion).isEqualTo(2L);
        assertThat(deleted).isEqualTo(2);
        assertThat(nbAfterDeletion).isEqualTo(0);
    }
}
