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
package io.gravitee.rest.api.service.v4.exception;

import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowSelectorsDuplicatedException extends AbstractManagementException {

    private final String name;
    private final Set<String> duplicatedSelectors;

    public FlowSelectorsDuplicatedException(final String name, final Set<String> duplicatedSelectors) {
        this.name = name;
        this.duplicatedSelectors = duplicatedSelectors;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "The flow [" + name + "] contains duplicated selectors type [" + duplicatedSelectors + "].";
    }

    @Override
    public String getTechnicalCode() {
        return "flow.selectors.type.duplicated";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("flow.selectors.type.duplicated", duplicatedSelectors.toString());
    }
}
