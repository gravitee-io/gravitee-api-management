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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.api.AsyncJobRepository.SearchCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.AsyncJob;
import java.util.Date;
import java.util.Optional;
import org.junit.Test;

public class AsyncJobRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/asyncjob-tests/";
    }

    // Create
    @Test
    public void should_create_a_job() throws TechnicalException {
        var date = new Date();
        var uuid = UUID.random().toString();
        var job = aJob(uuid, date);

        var createdIntegration = asyncJobRepository.create(job);

        assertThat(createdIntegration).isEqualTo(job);
    }

    // Find by id
    @Test
    public void should_get_job_by_id() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";
        var date = new Date(1470157767009L);
        var expectedIntegration = aJob(id, date);

        var result = asyncJobRepository.findById(id);

        assertThat(result).hasValue(expectedIntegration);
    }

    @Test
    public void should_return_empty_when_job_not_found() throws TechnicalException {
        var integration = asyncJobRepository.findById("not-existing-id");

        assertThat(integration).isNotPresent();
    }

    // Update
    @Test
    public void should_update_job() throws TechnicalException {
        var id = "459a022c-e79c-4411-9a02-2ce79c141165";
        var date = new Date(1470157767000L);
        var updateDate = new Date(1712660289);

        var toUpdate = aJob(id, date).toBuilder().status("ERROR").errorMessage("an error").updatedAt(updateDate).build();

        var result = asyncJobRepository.update(toUpdate);
        assertThat(result).isEqualTo(toUpdate);
    }

    @Test
    public void should_throw_exception_when_job_to_update_not_found() {
        var job = aJob("not-existing-id", new Date(1470157767000L));

        assertThatThrownBy(() -> asyncJobRepository.update(job)).isInstanceOf(Exception.class);
    }

    // Delete
    @Test
    public void should_delete_a_job() throws TechnicalException {
        var id = "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3";

        asyncJobRepository.delete(id);

        var result = asyncJobRepository.findById(id);
        assertThat(result).isEmpty();
    }

    // Find pending job
    @Test
    public void should_return_pending_job_for_source_id() throws TechnicalException {
        var result = asyncJobRepository.findPendingJobFor("source-id");

        assertThat(result)
            .hasValue(
                AsyncJob
                    .builder()
                    .id("f66274c9-3d8f-44c5-a274-c93d8fb4c5f3")
                    .sourceId("source-id")
                    .status("PENDING")
                    .type("FEDERATED_API_INGESTION")
                    .initiatorId("initiator-id")
                    .upperLimit(100L)
                    .environmentId("my-env")
                    .createdAt(new Date(1470157767009L))
                    .updatedAt(new Date(1470157767009L))
                    .build()
            );
    }

    @Test
    public void should_return_empty_when_no_pending_job_found() throws TechnicalException {
        assertThat(asyncJobRepository.findPendingJobFor("459a022c-e79c-4411-9a02-2ce79c141165")).isNotPresent();
        assertThat(asyncJobRepository.findPendingJobFor("unknown")).isNotPresent();
    }

    // Search
    @Test
    public void should_search_with_1_criteria() throws TechnicalException {
        Page<AsyncJob> result;

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.of("user-id"), Optional.empty(), Optional.empty(), Optional.empty())
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "cad107c9-27f2-40b2-9107-c927f2e0b2fc",
                "459a022c-e79c-4411-9a02-2ce79c141165",
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "0aa8629b-739c-4a69-bcc8-4b34d8fc5ae0",
                "1e4ace08-0658-46c4-8b79-42aaf026db63"
            );

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.empty(), Optional.of("SCORING_REQUEST"), Optional.empty(), Optional.empty())
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "0aa8629b-739c-4a69-bcc8-4b34d8fc5ae0",
                "1e4ace08-0658-46c4-8b79-42aaf026db63",
                "345ec3c1-bce7-4ad9-a2a0-e06dc86ab5fb"
            );

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.empty(), Optional.empty(), Optional.of("PENDING"), Optional.empty())
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "cad107c9-27f2-40b2-9107-c927f2e0b2fc",
                "f66274c9-3d8f-44c5-a274-c93d8fb4c5f3",
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "0aa8629b-739c-4a69-bcc8-4b34d8fc5ae0",
                "345ec3c1-bce7-4ad9-a2a0-e06dc86ab5fb"
            );
        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("api1"))
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "1e4ace08-0658-46c4-8b79-42aaf026db63",
                "345ec3c1-bce7-4ad9-a2a0-e06dc86ab5fb"
            );
    }

    @Test
    public void should_search_with_2_criteria() throws TechnicalException {
        Page<AsyncJob> result;

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.of("user-id"), Optional.of("SCORING_REQUEST"), Optional.empty(), Optional.empty())
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "0aa8629b-739c-4a69-bcc8-4b34d8fc5ae0",
                "1e4ace08-0658-46c4-8b79-42aaf026db63"
            );

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.empty(), Optional.of("SCORING_REQUEST"), Optional.of("PENDING"), Optional.empty())
            );
        assertThat(result.getContent())
            .extracting(AsyncJob::getId)
            .containsExactly(
                "0d9a5a6e-b384-4037-b285-54b4d9716e77",
                "0aa8629b-739c-4a69-bcc8-4b34d8fc5ae0",
                "345ec3c1-bce7-4ad9-a2a0-e06dc86ab5fb"
            );

        result =
            asyncJobRepository.search(
                new SearchCriteria("my-env", Optional.empty(), Optional.empty(), Optional.of("PENDING"), Optional.of("source-id"))
            );
        assertThat(result.getContent()).extracting(AsyncJob::getId).containsExactly("f66274c9-3d8f-44c5-a274-c93d8fb4c5f3");
    }

    @Test
    public void should_search_with_all_criteria() throws TechnicalException {
        Page<AsyncJob> result;

        result =
            asyncJobRepository.search(
                new SearchCriteria(
                    "my-env",
                    Optional.of("user-id"),
                    Optional.of("SCORING_REQUEST"),
                    Optional.of("PENDING"),
                    Optional.of("api1")
                )
            );
        assertThat(result.getContent()).extracting(AsyncJob::getId).containsExactly("0d9a5a6e-b384-4037-b285-54b4d9716e77");
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        long nbBeforeDeletion = asyncJobRepository.findAll().stream().filter(job -> "ToBeDeleted".equals(job.getEnvironmentId())).count();
        int deleted = asyncJobRepository.deleteByEnvironmentId("ToBeDeleted").size();
        long nbAfterDeletion = asyncJobRepository.findAll().stream().filter(job -> "ToBeDeleted".equals(job.getEnvironmentId())).count();

        assertThat(nbBeforeDeletion).isEqualTo(2L);
        assertThat(deleted).isEqualTo(2);
        assertThat(nbAfterDeletion).isEqualTo(0);
    }

    private static AsyncJob aJob(String uuid, Date date) {
        return AsyncJob
            .builder()
            .id(uuid)
            .sourceId("source-id")
            .status("PENDING")
            .type("FEDERATED_API_INGESTION")
            .initiatorId("initiator-id")
            .upperLimit(100L)
            .environmentId("my-env")
            .createdAt(date)
            .updatedAt(date)
            .build();
    }
}
