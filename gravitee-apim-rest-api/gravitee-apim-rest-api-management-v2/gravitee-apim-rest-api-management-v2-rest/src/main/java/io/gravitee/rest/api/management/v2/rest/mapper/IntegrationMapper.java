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

import static io.gravitee.apim.core.integration.use_case.DiscoveryUseCase.Output.State.NEW;
import static io.gravitee.apim.core.integration.use_case.DiscoveryUseCase.Output.State.UPDATE;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.use_case.DiscoveryUseCase;
import io.gravitee.apim.core.integration.use_case.UpdateIntegrationUseCase;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobStatus;
import io.gravitee.rest.api.management.v2.rest.model.CreateIntegration;
import io.gravitee.rest.api.management.v2.rest.model.IngestionJob;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponseApisInner;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class })
public interface IntegrationMapper {
    Logger logger = LoggerFactory.getLogger(IntegrationMapper.class);
    IntegrationMapper INSTANCE = Mappers.getMapper(IntegrationMapper.class);

    default Integration map(CreateIntegration source, String environmentId) {
        return "A2A".equals(source.getProvider())
            ? mapToA2aIntegration(source).withEnvironmentId(environmentId)
            : mapToApiIntegration(source).withEnvironmentId(environmentId);
    }

    Integration.ApiIntegration mapToApiIntegration(CreateIntegration source);

    Integration.A2aIntegration mapToA2aIntegration(CreateIntegration source);

    IngestionJob map(AsyncJob source);

    @Mapping(target = "id", expression = "java(source.id())")
    @Mapping(target = "name", expression = "java(source.name())")
    @Mapping(target = "description", expression = "java(source.description())")
    @Mapping(target = "provider", expression = "java(source.provider())")
    @Mapping(target = "groups", expression = "java(List.copyOf(source.groups()))")
    io.gravitee.rest.api.management.v2.rest.model.Integration map(Integration source);

    default io.gravitee.rest.api.management.v2.rest.model.Integration map(IntegrationView source) {
        return switch (source.getIntegration()) {
            case Integration.A2aIntegration a2aIntegration -> map(source, a2aIntegration);
            default -> map(source, source.getIntegration());
        };
    }

    @Mapping(target = "id", expression = "java(integration.id())")
    @Mapping(target = "name", expression = "java(integration.name())")
    @Mapping(target = "description", expression = "java(integration.description())")
    @Mapping(target = "provider", expression = "java(integration.provider())")
    @Mapping(target = "groups", expression = "java(List.copyOf(integration.groups()))")
    io.gravitee.rest.api.management.v2.rest.model.Integration map(IntegrationView source, Integration integration);

    @Mapping(target = "id", expression = "java(integration.id())")
    @Mapping(target = "name", expression = "java(integration.name())")
    @Mapping(target = "description", expression = "java(integration.description())")
    @Mapping(target = "provider", expression = "java(integration.provider())")
    @Mapping(target = "groups", expression = "java(List.copyOf(integration.groups()))")
    io.gravitee.rest.api.management.v2.rest.model.Integration map(IntegrationView source, Integration.A2aIntegration integration);

    List<io.gravitee.rest.api.management.v2.rest.model.Integration> map(Set<Integration> source);

    UpdateIntegrationUseCase.Input.UpdateFields map(io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration source);

    AsyncJobStatus map(AsyncJob.Status source);

    static IngestionPreviewResponse mapper(DiscoveryUseCase.Output preview) {
        return new IngestionPreviewResponse()
            .totalCount(preview.apis().size())
            .newCount(preview.apis().stream().filter(api -> api.state() == NEW).count())
            .updateCount(preview.apis().stream().filter(api -> api.state() == UPDATE).count())
            .apis(
                preview
                    .apis()
                    .stream()
                    .map(a ->
                        new IngestionPreviewResponseApisInner()
                            .id(a.id())
                            .name(a.name())
                            .version(a.version())
                            .state(
                                switch (a.state()) {
                                    case NEW -> IngestionPreviewResponseApisInner.StateEnum.NEW;
                                    case UPDATE -> IngestionPreviewResponseApisInner.StateEnum.UPDATE;
                                }
                            )
                    )
                    .toList()
            )
            .isPartiallyDiscovered(preview.isPartialDiscovery());
    }
}
