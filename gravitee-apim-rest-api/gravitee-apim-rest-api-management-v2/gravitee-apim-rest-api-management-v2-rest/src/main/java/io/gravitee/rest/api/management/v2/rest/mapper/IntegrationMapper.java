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

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.use_case.DiscoveryUseCase;
import io.gravitee.rest.api.management.v2.rest.model.CreateIntegration;
import io.gravitee.rest.api.management.v2.rest.model.IngestionJob;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponse;
import io.gravitee.rest.api.management.v2.rest.model.IngestionPreviewResponseApisInner;
import io.gravitee.rest.api.management.v2.rest.model.IngestionStatus;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
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

    Integration map(CreateIntegration source);

    IngestionJob map(IntegrationJob source);

    io.gravitee.rest.api.management.v2.rest.model.Integration map(Integration source);

    io.gravitee.rest.api.management.v2.rest.model.Integration map(IntegrationView source);

    List<io.gravitee.rest.api.management.v2.rest.model.Integration> map(Set<Integration> source);

    Integration map(io.gravitee.rest.api.management.v2.rest.model.UpdateIntegration source);

    IngestionStatus map(IntegrationJob.Status source);

    static IngestionPreviewResponse mapper(DiscoveryUseCase.Output preview) {
        return IngestionPreviewResponse
            .builder()
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
                            .state(
                                switch (a.state()) {
                                    case NEW -> IngestionPreviewResponseApisInner.StateEnum.NEW;
                                    case UPDATE -> IngestionPreviewResponseApisInner.StateEnum.UPDATE;
                                }
                            )
                    )
                    .toList()
            )
            .build();
    }
}
