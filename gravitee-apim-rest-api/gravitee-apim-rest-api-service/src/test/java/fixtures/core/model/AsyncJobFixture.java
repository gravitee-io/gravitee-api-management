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
package fixtures.core.model;

import io.gravitee.apim.core.integration.model.AsyncJob;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AsyncJobFixture {

    public static final Supplier<AsyncJob.AsyncJobBuilder> BASE = () ->
        AsyncJob
            .builder()
            .id("job-id")
            .sourceId("integration-id")
            .initiatorId("initiator-id")
            .environmentId("my-env")
            .status(AsyncJob.Status.PENDING)
            .upperLimit(10L)
            .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static AsyncJob aPendingFederatedApiIngestionJob() {
        return BASE.get().type(AsyncJob.Type.FEDERATED_APIS_INGESTION).build();
    }

    public static AsyncJob aSuccessFederatedApiIngestionJob() {
        return BASE.get().type(AsyncJob.Type.FEDERATED_APIS_INGESTION).status(AsyncJob.Status.SUCCESS).build();
    }

    public static AsyncJob anErrorFederatedApiIngestionJob() {
        return BASE.get().type(AsyncJob.Type.FEDERATED_APIS_INGESTION).status(AsyncJob.Status.ERROR).errorMessage("Job failed").build();
    }

    public static AsyncJob aPendingScoringRequestJob() {
        return BASE.get().type(AsyncJob.Type.SCORING_REQUEST).build();
    }
}
