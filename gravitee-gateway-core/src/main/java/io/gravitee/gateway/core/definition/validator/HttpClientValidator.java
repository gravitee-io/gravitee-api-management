/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.definition.validator;

import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.definition.HttpClientDefinition;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpClientValidator implements Validator {

    @Override
    public void validate(ApiDefinition definition) throws ValidationException {
        HttpClientDefinition httpClientDefinition = definition.getProxy().getHttpClient();

        if (httpClientDefinition.isUseProxy() && httpClientDefinition.getHttpProxy() == null) {
            throw new ValidationException("An API must have a HTTP proxy if 'use_proxy' property is enabled");
        }

    }
}
