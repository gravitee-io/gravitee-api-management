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

import io.gravitee.apim.core.integration.model.IntegrationJob;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IntegrationJobFixture {

    public static final Supplier<IntegrationJob.IntegrationJobBuilder> BASE = () ->
        IntegrationJob
            .builder()
            .id("job-id")
            .sourceId("integration-id")
            .initiatorId("initiator-id")
            .environmentId("my-env")
            .status(IntegrationJob.Status.PENDING)
            .upperLimit(10L)
            .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static IntegrationJob aPendingIngestJob() {
        return BASE.get().build();
    }

    public static IntegrationJob aSuccessJob() {
        return BASE.get().status(IntegrationJob.Status.SUCCESS).build();
    }

    public static IntegrationJob anErrorJob() {
        return BASE.get().status(IntegrationJob.Status.ERROR).errorMessage("Job failed").build();
    }
}
