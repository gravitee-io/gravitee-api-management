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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.application.model.crd.ApplicationCRDStatus;
import io.gravitee.apim.rest.api.automation.model.ApplicationSpec;
import io.gravitee.apim.rest.api.automation.model.ApplicationState;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.rest.api.management.v2.rest.mapper.DateMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.OriginContextMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSpec;
import io.gravitee.rest.api.model.ApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { DateMapper.class, OriginContextMapper.class, ServiceMapper.class })
public interface ApplicationMapper {
    ApplicationMapper INSTANCE = Mappers.getMapper(ApplicationMapper.class);

    ApplicationCRDSpec applicationSpecToApplicationCRDSpec(ApplicationSpec applicationSpec);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    ApplicationState applicationSpecToApplicationState(
        ApplicationSpec applicationSpec,
        String id,
        String organizationId,
        String environmentId
    );

    @Mapping(target = "id", source = "status.id")
    @Mapping(target = "errors", source = "status.errors")
    @Mapping(target = "organizationId", source = "status.organizationId")
    @Mapping(target = "environmentId", source = "status.environmentId")
    ApplicationState applicationSpecAndStatusToApplicationState(ApplicationSpec spec, ApplicationCRDStatus status);

    Errors toErrors(ApplicationCRDStatus.Errors errors);

    ApplicationSpec applicationCRDSpecToApplicationSpec(ApplicationCRDSpec spec);

    ApplicationSpec applicationEntityToApplicationSpec(ApplicationEntity applicationEntity);
}
