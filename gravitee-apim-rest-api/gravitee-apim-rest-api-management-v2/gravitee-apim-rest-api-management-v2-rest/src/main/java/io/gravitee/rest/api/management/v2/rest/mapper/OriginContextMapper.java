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

import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.model.context.OriginContext;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OriginContextMapper {
    OriginContextMapper INSTANCE = Mappers.getMapper(OriginContextMapper.class);

    default io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext map(OriginContext source) {
        if (source == null) {
            return null;
        }

        if (source instanceof OriginContext.Kubernetes kub) {
            return map(kub);
        } else if (source instanceof OriginContext.Management manage) {
            return map(manage);
        } else if (source instanceof OriginContext.Integration integration) {
            return map(integration);
        } else {
            return null;
        }
    }

    default OriginContext map(io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext source) {
        if (source == null || source.getOrigin() == null) {
            return new OriginContext.Management();
        }
        return switch (source.getOrigin()) {
            case MANAGEMENT -> map((ManagementOriginContext) source);
            case KUBERNETES -> map((KubernetesOriginContext) source);
            case INTEGRATION -> map((IntegrationOriginContext) source);
        };
    }

    ManagementOriginContext map(OriginContext.Management source);
    KubernetesOriginContext map(OriginContext.Kubernetes source);
    IntegrationOriginContext map(OriginContext.Integration source);
    OriginContext.Management map(ManagementOriginContext source);
    OriginContext.Kubernetes map(KubernetesOriginContext source);
    OriginContext.Integration map(IntegrationOriginContext source);
}
