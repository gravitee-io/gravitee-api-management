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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http.helper;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.service.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpDynamicPropertiesHelper {

    public static boolean canHandle(Api api) {
        return api != null && api.getServices() != null && isServiceEnabled(api.getServices().getDynamicProperty());
    }

    private static boolean isServiceEnabled(Service httpDynamicPropertiesService) {
        return (
            httpDynamicPropertiesService != null &&
            httpDynamicPropertiesService.isEnabled() &&
            HTTP_DYNAMIC_PROPERTIES_TYPE.equals(httpDynamicPropertiesService.getType())
        );
    }
}
