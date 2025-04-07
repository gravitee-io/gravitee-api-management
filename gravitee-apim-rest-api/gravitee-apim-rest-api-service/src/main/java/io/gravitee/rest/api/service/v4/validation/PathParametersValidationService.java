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
package io.gravitee.rest.api.service.v4.validation;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import java.util.stream.Stream;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 * @deprecated Use {@link io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService} instead
 */
@Deprecated
public interface PathParametersValidationService {
    void validate(ApiType apiType, Stream<FlowV4Impl> apiFlows, Stream<FlowV4Impl> planFlows);
}
