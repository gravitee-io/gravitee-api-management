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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService.ApiMetadataDecodeContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiMetadataDecodeContextMapper {

    public static ApiMetadataDecodeContext toApiMetadataDecodeContext(GenericApiEntity api) {
        return ApiMetadataDecodeContext.builder()
            .id(api.getId())
            .name(api.getName())
            .description(api.getDescription())
            .version(api.getApiVersion())
            .definitionVersion(api.getDefinitionVersion())
            .type(typeOf(api))
            .picture(api.getPicture())
            .primaryOwner(toPrimaryOwner(api.getPrimaryOwner()))
            .visibility(api.getVisibility() == null ? null : Api.Visibility.valueOf(api.getVisibility().name()))
            .state(toState(api.getState()))
            .lifecycleState(api.getLifecycleState() == null ? null : Api.ApiLifecycleState.valueOf(api.getLifecycleState().name()))
            .tags(api.getTags())
            .metadata(toMetadata(api.getMetadata()))
            .deployedAt(api.getDeployedAt())
            .createdAt(api.getCreatedAt())
            .updatedAt(api.getUpdatedAt())
            .build();
    }

    private static ApiType typeOf(GenericApiEntity api) {
        if (api instanceof ApiEntity v4Api) {
            return v4Api.getType();
        }
        if (api instanceof NativeApiEntity nativeApi) {
            return nativeApi.getType();
        }
        return null;
    }

    private static PrimaryOwnerApiTemplateData toPrimaryOwner(PrimaryOwnerEntity primaryOwner) {
        if (primaryOwner == null) {
            return null;
        }
        return new PrimaryOwnerApiTemplateData(
            primaryOwner.getId(),
            primaryOwner.getDisplayName(),
            primaryOwner.getEmail(),
            primaryOwner.getType()
        );
    }

    private static Api.LifecycleState toState(Lifecycle.State state) {
        if (state == null) {
            return null;
        }
        return state == Lifecycle.State.STARTED ? Api.LifecycleState.STARTED : Api.LifecycleState.STOPPED;
    }

    private static Map<String, String> toMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        var result = new HashMap<String, String>(metadata.size());
        metadata.forEach((key, value) -> result.put(key, value == null ? null : String.valueOf(value)));
        return result;
    }
}
