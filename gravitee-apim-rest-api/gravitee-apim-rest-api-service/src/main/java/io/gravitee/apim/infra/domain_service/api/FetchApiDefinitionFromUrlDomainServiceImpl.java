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
import io.gravitee.rest.api.service.exceptions.ApiDefinitionFetchException;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class FetchApiDefinitionFromUrlDomainServiceImpl implements FetchApiDefinitionFromUrlDomainService {

    private final HttpClientService httpClientService;

    public FetchApiDefinitionFromUrlDomainServiceImpl(HttpClientService httpClientService) {
        this.httpClientService = httpClientService;
    }

    @Override
    public String fetch(String url, List<String> whitelist, boolean allowPrivate) {
        // URL allow-listing runs before the fetch; its UrlForbiddenException (400) must propagate untouched.
        UrlSanitizerUtils.checkAllowed(url, whitelist, allowPrivate);

        log.debug("Fetching API definition from URL '{}'", url);
        try {
            Buffer buffer = httpClientService.request(HttpMethod.GET, url, null, null, null);
            String body = buffer == null ? "" : buffer.toString();
            log.debug("Fetched API definition from URL '{}' ({} bytes)", url, body.length());
            return body;
        } catch (RuntimeException e) {
            // Any failure raised while fetching (non-success status, unreachable host, DNS failure, …) is a
            // client-fixable problem with the supplied URL, not a server fault. Translate it to a clean 4xx and
            // keep the cause for server-side logging only — never echo it back to the caller.
            log.warn("Failed to fetch API definition from URL '{}'", url, e);
            throw new ApiDefinitionFetchException(e);
        }
    }
}
