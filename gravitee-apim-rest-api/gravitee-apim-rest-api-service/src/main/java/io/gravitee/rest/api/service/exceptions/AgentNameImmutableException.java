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
package io.gravitee.rest.api.service.exceptions;

import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Map;

/**
 * An agent application shares its name with the agent identity it acts for; the name is set at provisioning and a
 * rename would silently break that coupling, so it is refused rather than dropped.
 */
public class AgentNameImmutableException extends AbstractManagementException {

    private final String name;

    public AgentNameImmutableException(String name) {
        this.name = name;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "An agent application keeps the name of its agent [" + name + "]";
    }

    @Override
    public String getTechnicalCode() {
        return "application.agentName.immutable";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("name", name);
    }
}
