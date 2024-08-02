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
package io.gravitee.apim.core.parameters.domain_service;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;

public interface ParametersDomainService {
    Map<Key, String> getSystemParameters(List<Key> keys);
    Map<Key, String> getEnvironmentParameters(ExecutionContext executionContext, List<Key> keys);
}
