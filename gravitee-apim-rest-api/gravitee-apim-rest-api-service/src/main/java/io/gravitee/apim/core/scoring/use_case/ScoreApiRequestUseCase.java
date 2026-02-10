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
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.scoring.model.ScoreRequest;
import io.gravitee.apim.core.scoring.model.ScoringAssetType;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.core.scoring.query_service.ScoringFunctionQueryService;
import io.gravitee.apim.core.scoring.query_service.ScoringRulesetQueryService;
import io.gravitee.apim.core.scoring.service_provider.ScoringProvider;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
@UseCase
public class ScoreApiRequestUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final ApiExportDomainService apiExportDomainService;
    private final GraviteeDefinitionSerializer graviteeDefinitionSerializer;
    private final ScoringProvider scoringProvider;
    private final AsyncJobCrudService asyncJobCrudService;
    private final ScoringRulesetQueryService scoringRulesetQueryService;
    private final ScoringFunctionQueryService scoringFunctionQueryService;

    public Completable execute(Input input) {
        var pages$ = Flowable.fromIterable(apiDocumentationDomainService.getApiPages(input.apiId, null))
            .filter(page -> page.isAsyncApi() || page.isSwagger())
            .map(this::assetToScore);
        var customRulesets$ = Flowable.fromCallable(() ->
            scoringRulesetQueryService.findByReference(input.auditInfo.environmentId(), ScoringRuleset.ReferenceType.ENVIRONMENT)
        )
            .flatMap(Flowable::fromIterable)
            .flatMapMaybe(this::customRuleset)
            .toList();
        var customFunctions$ = Flowable.fromCallable(() ->
            scoringFunctionQueryService.findByReference(input.auditInfo.environmentId(), ScoringFunction.ReferenceType.ENVIRONMENT)
        )
            .flatMap(Flowable::fromIterable)
            .map(r -> new ScoreRequest.Function(r.name(), r.payload()))
            .toList();

        var export$ = Flowable.fromCallable(() ->
            apiExportDomainService.export(input.apiId, input.auditInfo, EnumSet.noneOf(Excludable.class))
        )
            .map(this::assetToScore)
            .doOnError(throwable -> log.error("An error occurs while exporting API [{}]", input.apiId, throwable));

        return Maybe.fromOptional(apiCrudService.findById(input.apiId()))
            .subscribeOn(Schedulers.io())
            .switchIfEmpty(Single.error(new ApiNotFoundException(input.apiId())))
            .flatMap(api -> Flowable.merge(pages$, export$).toList())
            .flatMap(assets ->
                Single.zip(customRulesets$, customFunctions$, RulesetAndFunctions::new).map(entry ->
                    new ScoreRequest(
                        UuidString.generateRandom(),
                        input.auditInfo.organizationId(),
                        input.auditInfo.environmentId(),
                        input.apiId,
                        assets,
                        entry.rulesets(),
                        entry.functions()
                    )
                )
            )
            .flatMap(request ->
                Single.fromCallable(() ->
                    asyncJobCrudService.create(newScoringJob(request.jobId(), input.auditInfo, input.apiId, deadLine(request)))
                ).map(job -> Map.entry(request, job))
            )
            .flatMapCompletable(entry -> {
                var request = entry.getKey();
                var job = entry.getValue();
                return scoringProvider
                    .requestScore(request)
                    .onErrorResumeNext(throwable ->
                        Completable.fromRunnable(() -> asyncJobCrudService.update(job.error(throwable.getMessage()))).andThen(
                            Completable.error(throwable)
                        )
                    );
            });
    }

    private ScoreRequest.AssetToScore assetToScore(Page page) {
        return new ScoreRequest.AssetToScore(
            page.getId(),
            new ScoreRequest.AssetType(ScoringAssetType.fromPageType(page.getType())),
            page.getName(),
            page.getContent()
        );
    }

    private ScoreRequest.AssetToScore assetToScore(GraviteeDefinition definition) throws JsonProcessingException {
        return new ScoreRequest.AssetToScore(
            definition.api().id(),
            new ScoreRequest.AssetType(ScoringAssetType.GRAVITEE_DEFINITION, getFormat(definition)),
            definition.api().name(),
            graviteeDefinitionSerializer.serialize(definition)
        );
    }

    private static ScoreRequest.Format getFormat(GraviteeDefinition definition) {
        return switch (definition) {
            case GraviteeDefinition.V2 ignored -> ScoreRequest.Format.GRAVITEE_V2;
            case GraviteeDefinition.Federated ignored -> ScoreRequest.Format.GRAVITEE_FEDERATED;
            case GraviteeDefinition.Native ignored -> ScoreRequest.Format.GRAVITEE_NATIVE;
            case GraviteeDefinition.V4 v4 -> switch (v4.api().type()) {
                case A2A_PROXY, LLM_PROXY, MCP_PROXY, PROXY -> ScoreRequest.Format.GRAVITEE_PROXY;
                case MESSAGE -> ScoreRequest.Format.GRAVITEE_MESSAGE;
                case NATIVE -> ScoreRequest.Format.GRAVITEE_NATIVE;
            };
        };
    }

    private Maybe<ScoreRequest.CustomRuleset> customRuleset(ScoringRuleset scoringRuleset) {
        return StringUtils.isEmpty(scoringRuleset.payload())
            ? Maybe.empty()
            : Maybe.just(new ScoreRequest.CustomRuleset(scoringRuleset.payload(), format(scoringRuleset.format())));
    }

    private ScoreRequest.Format format(ScoringRuleset.Format format) {
        return switch (format) {
            case null -> null;
            case GRAVITEE_FEDERATION -> ScoreRequest.Format.GRAVITEE_FEDERATED;
            case GRAVITEE_MESSAGE -> ScoreRequest.Format.GRAVITEE_MESSAGE;
            case GRAVITEE_PROXY -> ScoreRequest.Format.GRAVITEE_PROXY;
            case GRAVITEE_NATIVE -> ScoreRequest.Format.GRAVITEE_NATIVE;
            case GRAVITEE_V2 -> ScoreRequest.Format.GRAVITEE_V2;
            case OPENAPI, ASYNCAPI -> null;
        };
    }

    public AsyncJob newScoringJob(String id, AuditInfo auditInfo, String apiId, Duration ttl) {
        var now = TimeProvider.now();
        return AsyncJob.builder()
            .id(id)
            .sourceId(apiId)
            .environmentId(auditInfo.environmentId())
            .initiatorId(auditInfo.actor().userId())
            .type(AsyncJob.Type.SCORING_REQUEST)
            .status(AsyncJob.Status.PENDING)
            .upperLimit(1L)
            .createdAt(now)
            .updatedAt(now)
            .deadLine(now.plus(ttl))
            .build();
    }

    public record Input(String apiId, AuditInfo auditInfo) {}

    private record RulesetAndFunctions(List<ScoreRequest.CustomRuleset> rulesets, List<ScoreRequest.Function> functions) {}

    /**
     * Compute the TTL of the scoring job.
     * <p>
     * As simple rule, we consider that the scoring job will take 10 seconds per asset per ruleset to score (as simple ) and 30 seconds for the margin.
     *
     * @param request the scoring request
     * @return the TTL of the scoring job
     */
    private Duration deadLine(ScoreRequest request) {
        var margin = Duration.ofSeconds(30);
        int nbAssets = request.assets().size();
        int rulesets = Math.min(1, request.customRulesets().size());
        int numberOfRuns = nbAssets * rulesets;
        return Duration.ofSeconds(10).multipliedBy(numberOfRuns).plus(margin);
    }
}
