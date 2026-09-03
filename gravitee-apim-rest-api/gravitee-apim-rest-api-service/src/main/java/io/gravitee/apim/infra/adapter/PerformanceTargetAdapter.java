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

import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import java.time.Duration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PerformanceTargetAdapter {
    PerformanceTargetAdapter INSTANCE = Mappers.getMapper(PerformanceTargetAdapter.class);

    @Mapping(target = "subject", source = "source")
    @Mapping(target = "window", source = "windowSeconds")
    @Mapping(target = "interval", source = "intervalSeconds")
    PerformanceTarget toEntity(io.gravitee.repository.management.model.PerformanceTarget source);

    PerformanceTarget.Subject toSubject(io.gravitee.repository.management.model.PerformanceTarget source);

    @Mapping(target = "apiIds", source = "subject.apiIds")
    @Mapping(target = "reference", source = "subject.reference")
    @Mapping(target = "windowSeconds", source = "window")
    @Mapping(target = "intervalSeconds", source = "interval")
    io.gravitee.repository.management.model.PerformanceTarget toRepository(PerformanceTarget source);

    default Duration toDuration(long seconds) {
        return Duration.ofSeconds(seconds);
    }

    default long toSeconds(Duration duration) {
        return duration.getSeconds();
    }
}
