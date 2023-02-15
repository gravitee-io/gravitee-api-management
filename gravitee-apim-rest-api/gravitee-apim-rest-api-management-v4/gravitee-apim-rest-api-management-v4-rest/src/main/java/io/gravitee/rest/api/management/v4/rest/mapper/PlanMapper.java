/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v4.rest.mapper;

import io.gravitee.rest.api.management.v4.rest.model.Plan;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlanMapper {
    PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);

    Plan convert(PlanEntity planEntity);
    List<Plan> convertList(List<PlanEntity> planEntityList);

    default OffsetDateTime map(Date value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
