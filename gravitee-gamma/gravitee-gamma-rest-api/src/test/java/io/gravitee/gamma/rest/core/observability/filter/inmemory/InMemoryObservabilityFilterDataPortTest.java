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
package io.gravitee.gamma.rest.core.observability.filter.inmemory;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.port.ObservabilityFilterDataPortContractTest;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import java.util.List;
import java.util.Map;

class InMemoryObservabilityFilterDataPortTest extends ObservabilityFilterDataPortContractTest {

    private final InMemoryObservabilityFilterDataPort port = new InMemoryObservabilityFilterDataPort();

    @Override
    protected ObservabilityFilterDataPort port() {
        return port;
    }

    @Override
    protected void givenKeywordValues(String filterName, List<FilterValue> values) {
        port.givenKeywordValues(filterName, values);
    }

    @Override
    protected void givenLabels(String filterName, Map<String, String> labels) {
        port.givenLabels(filterName, labels);
    }
}
