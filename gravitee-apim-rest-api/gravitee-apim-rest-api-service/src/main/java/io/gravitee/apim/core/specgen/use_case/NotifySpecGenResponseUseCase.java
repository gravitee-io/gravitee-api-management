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
package io.gravitee.apim.core.specgen.use_case;

import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.SpecGenNotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
@Slf4j
public class NotifySpecGenResponseUseCase {

    private final ApiSpecGenQueryService apiSpecGenQueryService;
    private final SpecGenNotificationProvider specGenNotificationProvider;

    public void notify(String apiId, String userId) {
        apiSpecGenQueryService
            .rxFindByIdAndType(getExecutionContext(), apiId, PROXY)
            .subscribe(
                apiSpecGen -> specGenNotificationProvider.notify(apiSpecGen, userId),
                t -> log.error("An unexpected error has occurred", t)
            );
    }
}
