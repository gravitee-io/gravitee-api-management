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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
public class GenericApiMapper {

    private final ApiMapper apiMapper;
    private final ApiConverter apiConverter;

    public GenericApiEntity toGenericApi(final Api api, final PrimaryOwnerEntity primaryOwner) {
        return switch (getVersionOfDefault(api)) {
            case V4 -> switch (api.getType()) {
                case NATIVE -> apiMapper.toNativeEntity(api, primaryOwner);
                case MESSAGE, PROXY -> apiMapper.toEntity(api, primaryOwner);
            };
            case FEDERATED -> apiMapper.federatedToEntity(api, primaryOwner);
            case V1, V2 -> apiConverter.toApiEntity(api, primaryOwner);
        };
    }

    public GenericApiEntity toGenericApi(final ExecutionContext executionContext, final Api api, final PrimaryOwnerEntity primaryOwner) {
        return switch (getVersionOfDefault(api)) {
            case V4 -> switch (api.getType()) {
                case NATIVE -> apiMapper.toNativeEntity(executionContext, api, primaryOwner, true);
                case MESSAGE, PROXY -> apiMapper.toEntity(executionContext, api, primaryOwner, true);
            };
            case FEDERATED -> apiMapper.federatedToEntity(executionContext, api, primaryOwner);
            case V1, V2 -> apiConverter.toApiEntity(executionContext, api, primaryOwner, true);
        };
    }

    private DefinitionVersion getVersionOfDefault(final Api api) {
        return api.getDefinitionVersion() == null ? DefinitionVersion.V2 : api.getDefinitionVersion();
    }
}
