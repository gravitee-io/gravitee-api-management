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

import io.gravitee.definition.model.HttpClient;
import io.gravitee.gateway.core.definition.Api;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpClientValidator implements Validator {

    @Override
    public void validate(Api definition) throws ValidationException {
        HttpClient httpClient = definition.getProxy().getHttpClient();

        if (httpClient != null && httpClient.isUseProxy() && httpClient.getHttpProxy() == null) {
            throw new ValidationException("An API must have a HTTP proxy if 'use_proxy' property is enabled");
        }

    }
}
