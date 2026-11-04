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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.model.AsyncJobError;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper
public interface AsyncJobAdapter {
    AsyncJobAdapter INSTANCE = Mappers.getMapper(AsyncJobAdapter.class);
    Logger log = NodeLoggerFactory.getLogger(AsyncJobAdapter.class);

    /** Job rows can be written by other modules: unknown fields are ignored rather than failing every read. */
    ObjectMapper ERRORS_JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mapping(target = "errors", ignore = true)
    AsyncJob toEntity(io.gravitee.repository.management.model.AsyncJob source);

    @Mapping(target = "errors", source = "errors", qualifiedByName = "errorsToJson")
    io.gravitee.repository.management.model.AsyncJob toRepository(AsyncJob source);

    /**
     * Unreadable errors are logged and dropped so that one corrupted job never breaks the reads of the other jobs
     * (listing, timeout checks, pending-job lookup).
     */
    @AfterMapping
    default void readErrors(io.gravitee.repository.management.model.AsyncJob source, @MappingTarget AsyncJob target) {
        var json = source.getErrors();
        if (json == null || json.isBlank()) {
            target.setErrors(List.of());
            return;
        }
        try {
            List<AsyncJobError> errors = ERRORS_JSON.readValue(json, new TypeReference<>() {});
            target.setErrors(errors == null ? List.of() : errors);
        } catch (JsonProcessingException e) {
            log.warn("Ignoring unreadable errors of async job {}", source.getId(), e);
            target.setErrors(List.of());
        }
    }

    /** Stores at most {@link AsyncJob#MAX_ERRORS} errors, keeping the first ones. */
    @Named("errorsToJson")
    default String errorsToJson(List<AsyncJobError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        try {
            return ERRORS_JSON.writeValueAsString(errors.size() > AsyncJob.MAX_ERRORS ? errors.subList(0, AsyncJob.MAX_ERRORS) : errors);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize async job errors", e);
        }
    }
}
