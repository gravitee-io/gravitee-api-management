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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.model.AsyncJobError;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AsyncJobAdapter {
    AsyncJobAdapter INSTANCE = Mappers.getMapper(AsyncJobAdapter.class);

    ObjectMapper ERRORS_JSON = new ObjectMapper();

    @Mapping(target = "errors", source = "errors", qualifiedByName = "jsonToErrors")
    AsyncJob toEntity(io.gravitee.repository.management.model.AsyncJob source);

    @Mapping(target = "errors", source = "errors", qualifiedByName = "errorsToJson")
    io.gravitee.repository.management.model.AsyncJob toRepository(AsyncJob source);

    @Named("jsonToErrors")
    default List<AsyncJobError> jsonToErrors(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return ERRORS_JSON.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize async job errors", e);
        }
    }

    @Named("errorsToJson")
    default String errorsToJson(List<AsyncJobError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        try {
            return ERRORS_JSON.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize async job errors", e);
        }
    }
}
