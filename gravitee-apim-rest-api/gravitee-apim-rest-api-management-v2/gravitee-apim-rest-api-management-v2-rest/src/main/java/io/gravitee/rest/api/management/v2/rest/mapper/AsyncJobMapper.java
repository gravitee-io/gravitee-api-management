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
package io.gravitee.rest.api.management.v2.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJob;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobStatus;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobType;
import java.util.List;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper(uses = { DateMapper.class })
public interface AsyncJobMapper {
    Logger log = NodeLoggerFactory.getLogger(AsyncJobMapper.class);
    AsyncJobMapper INSTANCE = Mappers.getMapper(AsyncJobMapper.class);
    GraviteeMapper DETAILS_JSON = new GraviteeMapper();

    @Mapping(target = "details", ignore = true)
    AsyncJob map(io.gravitee.apim.core.async_job.model.AsyncJob source);

    List<AsyncJob> map(List<io.gravitee.apim.core.async_job.model.AsyncJob> source);

    io.gravitee.apim.core.async_job.model.AsyncJob.Type map(AsyncJobType source);
    io.gravitee.apim.core.async_job.model.AsyncJob.Status map(AsyncJobStatus source);

    /**
     * Details are written by the job's owner, possibly another module: blank or unreadable details are logged and left
     * out, so that one job never fails the listing of the others.
     */
    @AfterMapping
    default void readDetails(io.gravitee.apim.core.async_job.model.AsyncJob source, @MappingTarget AsyncJob target) {
        var details = source.getDetails();
        if (details == null || details.isBlank()) {
            return;
        }
        try {
            Map<String, Object> parsed = DETAILS_JSON.readValue(details, new TypeReference<>() {});
            target.setDetails(parsed);
        } catch (JsonProcessingException e) {
            log.warn("Ignoring unreadable details of async job {}", source.getId(), e);
        }
    }
}
