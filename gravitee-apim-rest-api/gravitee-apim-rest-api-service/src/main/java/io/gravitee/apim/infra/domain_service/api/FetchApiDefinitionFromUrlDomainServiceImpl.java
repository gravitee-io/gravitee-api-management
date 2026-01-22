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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.FetchApiDefinitionFromUrlDomainService;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.vertx.core.buffer.Buffer;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class FetchApiDefinitionFromUrlDomainServiceImpl implements FetchApiDefinitionFromUrlDomainService {

    private final HttpClientService httpClientService;

    public FetchApiDefinitionFromUrlDomainServiceImpl(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Override
    public String fetch(String url, java.util.List<String> whitelist, boolean allowPrivate) {
        UrlSanitizerUtils.checkAllowed(url, whitelist, allowPrivate);

        try {
            Buffer buffer = httpClientService.request(HttpMethod.GET, url, null, null, null);
            return buffer.toString();
        } catch (Exception e) {
            throw new TechnicalManagementException("Failed to fetch API definition from URL: " + e.getMessage(), e);
        }
    }
}
