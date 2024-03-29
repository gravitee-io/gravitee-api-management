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
package io.gravitee.rest.api.service.v4.exception;

import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ListenerEntrypointDuplicatedException extends AbstractManagementException {

    private final ListenerType listenerType;
    private final Set<String> duplicatedEntrypoints;

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "The api contains duplicated entrypoints [" + duplicatedEntrypoints + "] for listener [" + listenerType.getLabel() + "].";
    }

    @Override
    public String getTechnicalCode() {
        return "api.listeners.entrypoints.type.duplicated";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("api.listeners.entrypoints.type.duplicated", duplicatedEntrypoints.toString());
    }
}
