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
package io.gravitee.apim.core.api.model.factory;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApi;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;

public class ApiModelFactory {

    private ApiModelFactory() {}

    public static Api fromNewApi(NewApi newApi, String environmentId) {
        var id = UuidString.generateRandom();
        var now = TimeProvider.now();
        return newApi
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionV4(newApi.toApiDefinitionBuilder().id(id).build())
            .build();
    }

    public static Api fromCrd(ApiCRD crd, String environmentId) {
        var id = crd.getId() != null ? crd.getId() : UuidString.generateRandom();
        var now = TimeProvider.now();
        return crd
            .toApiBuilder()
            .id(id)
            .environmentId(environmentId)
            .createdAt(now)
            .updatedAt(now)
            .apiDefinitionV4(crd.toApiDefinitionBuilder().id(id).build())
            .build();
    }
}
