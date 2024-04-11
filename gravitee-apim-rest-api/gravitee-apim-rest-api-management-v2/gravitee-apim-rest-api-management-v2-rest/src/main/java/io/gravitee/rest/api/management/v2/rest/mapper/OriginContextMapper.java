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

import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.model.context.IntegrationContext;
import io.gravitee.rest.api.model.context.KubernetesContext;
import io.gravitee.rest.api.model.context.ManagementContext;
import io.gravitee.rest.api.model.context.OriginContext;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OriginContextMapper {
    OriginContextMapper INSTANCE = Mappers.getMapper(OriginContextMapper.class);

    default io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext map(OriginContext source) {
        if (source == null) {
            return null;
        }

        return switch (source.getOrigin()) {
            case MANAGEMENT -> map((ManagementContext) source);
            case KUBERNETES -> map((KubernetesContext) source);
            case INTEGRATION -> map((IntegrationContext) source);
        };
    }

    default OriginContext map(io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext source) {
        if (source == null || source.getOrigin() == null) {
            return new ManagementContext();
        }
        return switch (source.getOrigin()) {
            case MANAGEMENT -> map((ManagementOriginContext) source);
            case KUBERNETES -> map((KubernetesOriginContext) source);
            case INTEGRATION -> map((IntegrationOriginContext) source);
        };
    }

    ManagementOriginContext map(ManagementContext source);
    KubernetesOriginContext map(KubernetesContext source);
    IntegrationOriginContext map(IntegrationContext source);
    ManagementContext map(ManagementOriginContext source);
    KubernetesContext map(KubernetesOriginContext source);
    IntegrationContext map(IntegrationOriginContext source);
}
