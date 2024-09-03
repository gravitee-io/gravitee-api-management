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
package io.gravitee.apim.core.integration.model;

import io.gravitee.common.utils.TimeProvider;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AsyncJob {

    String id;

    /**
     * The component id that triggers the job.
     * For example, if the job is about ingesting Federated APIs from an external source, this id will be the id of the Integration.
     */
    @With
    String sourceId;

    /** The environment id where the job is executed */
    String environmentId;

    /** The user id that triggers the job */
    String initiatorId;

    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;

    Status status;

    /** The error message if the job failed */
    String errorMessage;

    /**
     * The upper limit telling how many items can be processed by this job.
     * For example, if the job is about ingesting Federated APIs from an external source, this limit will be the number of APIs to ingest.
     */
    Long upperLimit;

    public enum Status {
        PENDING,
        SUCCESS,
        ERROR,
    }

    public AsyncJob complete() {
        return toBuilder().status(Status.SUCCESS).updatedAt(TimeProvider.now()).build();
    }
}
