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
package io.gravitee.gamma.module.platform.rest.resource.mapper.am;

import io.gravitee.gamma.module.platform.core.am.model.AmModels.AmConnectionTestResult;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.Domain;
import io.gravitee.gamma.module.platform.core.am.model.AmModels.Environment;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionTestResultResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.DomainResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.EnvironmentResponse;

public final class AmDtoMapper {

    private AmDtoMapper() {}

    public static EnvironmentResponse toDto(Environment e) {
        return new EnvironmentResponse(e.id(), e.name());
    }

    public static DomainResponse toDto(Domain d) {
        return new DomainResponse(d.id(), d.name(), d.hrid());
    }

    public static AmConnectionTestResultResponse toDto(AmConnectionTestResult r) {
        return new AmConnectionTestResultResponse(r.ok(), r.status(), r.message());
    }
}
