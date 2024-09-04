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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.integration.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.service_provider.ScoringProvider;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@UseCase
public class ScoreApiRequestUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final ScoringProvider scoringProvider;
    private final AsyncJobCrudService asyncJobCrudService;

    public ScoreApiRequestUseCase(
        ApiCrudService apiCrudService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        ScoringProvider scoringProvider,
        AsyncJobCrudService asyncJobCrudService
    ) {
        this.apiCrudService = apiCrudService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.scoringProvider = scoringProvider;
        this.asyncJobCrudService = asyncJobCrudService;
    }

    public Completable execute(Input input) {
        return Maybe
            .fromOptional(apiCrudService.findById(input.apiId()))
            .switchIfEmpty(Single.error(new ApiNotFoundException(input.apiId())))
            .flatMap(api ->
                Flowable
                    .fromIterable(apiDocumentationDomainService.getApiPages(api.getId(), null))
                    .filter(page -> page.isAsyncApi() || page.isSwagger())
                    .map(page ->
                        new ScoreRequest.AssetToScore(
                            page.getId(),
                            ScoringAssetType.fromPageType(page.getType()),
                            page.getName(),
                            page.getContent()
                        )
                    )
                    .toList()
            )
            .flatMapCompletable(assets -> {
                if (assets.isEmpty()) {
                    return Completable.complete();
                }
                var job = newScoringJob(UuidString.generateRandom(), input.auditInfo, input.apiId);
                return scoringProvider
                    .requestScore(
                        new ScoreRequest(
                            job.getId(),
                            input.auditInfo.organizationId(),
                            input.auditInfo.environmentId(),
                            input.apiId,
                            assets
                        )
                    )
                    .doOnComplete(() -> asyncJobCrudService.create(job));
            });
    }

    public AsyncJob newScoringJob(String id, AuditInfo auditInfo, String apiId) {
        var now = TimeProvider.now();
        return AsyncJob
            .builder()
            .id(id)
            .sourceId(apiId)
            .environmentId(auditInfo.environmentId())
            .initiatorId(auditInfo.actor().userId())
            .type(AsyncJob.Type.SCORING_REQUEST)
            .status(AsyncJob.Status.PENDING)
            .upperLimit(1L)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public record Input(String apiId, AuditInfo auditInfo) {}
}
