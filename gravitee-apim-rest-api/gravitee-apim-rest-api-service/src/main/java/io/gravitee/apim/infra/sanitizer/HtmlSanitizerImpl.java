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
package io.gravitee.apim.infra.sanitizer;

import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlSanitizerImpl implements HtmlSanitizer {

    @Override
    public String sanitize(String content) {
        return io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.sanitize(content);
    }

    @Override
    public SanitizeResult isSafe(String content) {
        var sanitizeInfos = io.gravitee.rest.api.service.sanitizer.HtmlSanitizer.isSafe(content);
        return SanitizeResult.builder().safe(sanitizeInfos.isSafe()).rejectedMessage(sanitizeInfos.getRejectedMessage()).build();
    }
}
