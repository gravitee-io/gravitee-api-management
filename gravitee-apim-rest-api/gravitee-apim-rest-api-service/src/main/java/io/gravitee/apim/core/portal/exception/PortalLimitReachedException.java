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
package io.gravitee.apim.core.portal.exception;

import io.gravitee.apim.core.exception.NotAllowedDomainException;
import java.util.Map;

public class PortalLimitReachedException extends NotAllowedDomainException {

    private final int limit;

    public PortalLimitReachedException(int limit) {
        super("The license allows %d %s in this environment".formatted(limit, limit == 1 ? "portal" : "portals"));
        this.limit = limit;
    }

    public String getTechnicalCode() {
        return "portal.limitReached";
    }

    public Map<String, String> getParameters() {
        return Map.of("limit", String.valueOf(limit));
    }
}
