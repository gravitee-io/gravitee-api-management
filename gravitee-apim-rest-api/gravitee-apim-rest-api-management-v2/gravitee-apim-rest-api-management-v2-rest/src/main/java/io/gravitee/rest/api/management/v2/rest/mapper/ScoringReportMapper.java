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

import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.model.ScoringReportView;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoring;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentApiScore;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import jakarta.ws.rs.core.UriInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface ScoringReportMapper {
    Logger logger = LoggerFactory.getLogger(ScoringReportMapper.class);
    ScoringReportMapper INSTANCE = Mappers.getMapper(ScoringReportMapper.class);

    ApiScoring map(ScoringReportView source);

    @Mapping(target = "id", source = "source.api.apiId")
    @Mapping(target = "name", source = "source.api.name")
    @Mapping(target = "errors", source = "source.summary.errors")
    @Mapping(target = "warnings", source = "source.summary.warnings")
    @Mapping(target = "infos", source = "source.summary.infos")
    @Mapping(target = "hints", source = "source.summary.hints")
    @Mapping(target = "pictureUrl", expression = "java(computePictureUrl(source, uriInfo))")
    EnvironmentApiScore map(EnvironmentApiScoringReport source, UriInfo uriInfo);

    @Named("computeApiLinks")
    default String computePictureUrl(EnvironmentApiScoringReport report, UriInfo uriInfo) {
        return ManagementApiLinkHelper.apiPictureURL(uriInfo.getBaseUriBuilder(), report);
    }
}
