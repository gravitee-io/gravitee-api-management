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

import static io.gravitee.apim.core.specgen.model.ApiSpecGenOperation.GET_STATE;
import static io.gravitee.apim.core.specgen.model.ApiSpecGenRequestState.UNAVAILABLE;
import static io.gravitee.definition.model.v4.ApiType.PROXY;
import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.specgen.model.ApiSpecGenOperation;
import io.gravitee.apim.core.specgen.model.ApiSpecGenState;
import io.gravitee.apim.core.specgen.query_service.ApiSpecGenQueryService;
import io.gravitee.apim.core.specgen.service_provider.SpecGenProvider;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class SpecGenRequestUseCase {

    private final ApiSpecGenQueryService apiSpecGenQueryService;
    private final SpecGenProvider specGenProvider;

    public Single<ApiSpecGenState> getState(String apiId) {
        return performRequest(apiId, GET_STATE);
    }

    public Single<ApiSpecGenState> postJob(String apiId) {
        return performRequest(apiId, ApiSpecGenOperation.POST_JOB);
    }

    private Single<ApiSpecGenState> performRequest(String apiId, ApiSpecGenOperation operation) {
        return apiSpecGenQueryService
            .findByIdAndType(getExecutionContext(), apiId, PROXY)
            .map(api -> specGenProvider.performRequest(apiId, operation).map(reply -> new ApiSpecGenState(reply.requestState())))
            .orElse(Single.just(new ApiSpecGenState(UNAVAILABLE)));
    }
}
