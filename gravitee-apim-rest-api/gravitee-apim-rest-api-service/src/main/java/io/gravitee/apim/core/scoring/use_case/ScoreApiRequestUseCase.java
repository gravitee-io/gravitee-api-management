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

package io.gravitee.apim.core.scoring.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.scoring.service_provider.ScoringProvider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@UseCase
public class ScoreApiRequestUseCase {

    private final ApiCrudService apiCrudService;
    private final ScoringProvider scoreDomainService;

    public ScoreApiRequestUseCase(ApiCrudService apiCrudService, ScoringProvider scoringProvider) {
        this.apiCrudService = apiCrudService;
        this.scoreDomainService = scoringProvider;
    }

    public Completable execute(Input input) {
        return Maybe
            .fromOptional(apiCrudService.findById(input.apiId()))
            .switchIfEmpty(Single.error(new ApiNotFoundException(input.apiId())))
            .flatMapCompletable(api -> this.scoreDomainService.requestScore(input.apiId(), input.envId(), input.orgId()));
    }

    public record Input(String apiId, String envId, String orgId) {}
}
