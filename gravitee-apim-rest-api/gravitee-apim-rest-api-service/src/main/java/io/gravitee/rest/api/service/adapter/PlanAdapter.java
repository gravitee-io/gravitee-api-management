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
package io.gravitee.rest.api.service.adapter;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Locale;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlanAdapter {
    PlanAdapter INSTANCE = Mappers.getMapper(PlanAdapter.class);

    default PlanEntity map(GenericPlanEntity entity) {
        return switch (entity) {
            case PlanEntity p -> p;
            case io.gravitee.rest.api.model.v4.plan.PlanEntity v4 -> map(v4);
            case NativePlanEntity nativePlan -> map(nativePlan);
            case null, default -> null;
        };
    }

    PlanEntity map(io.gravitee.rest.api.model.v4.plan.PlanEntity v4);
    PlanEntity map(NativePlanEntity v4);

    default PlanSecurityType map(PlanSecurity value) {
        return value == null ? null : PlanSecurityType.valueOf(value.getType().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
