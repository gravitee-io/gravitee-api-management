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
import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.scoring.crud_service.ScoringReportCrudService;
import io.gravitee.apim.core.scoring.domain_service.ScoreComputingDomainService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.common.utils.TimeProvider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SaveScoringResponseUseCase {

    private final AsyncJobCrudService asyncJobCrudService;
    private final ScoringReportCrudService scoringReportCrudService;
    private final ScoreComputingDomainService scoreComputingDomainService;

    public Completable execute(Input input) {
        return Maybe
            .defer(() -> Maybe.fromOptional(asyncJobCrudService.findById(input.jobId)))
            .subscribeOn(Schedulers.computation())
            .flatMapCompletable(job -> {
                String apiId = job.getSourceId();
                String environmentId = job.getEnvironmentId();

                var report = ScoringReport
                    .builder()
                    .id(job.getId())
                    .apiId(apiId)
                    .environmentId(environmentId)
                    .createdAt(TimeProvider.now())
                    .summary(processSummary(input.analyzedAssets))
                    .assets(input.analyzedAssets)
                    .build();

                return Completable
                    .fromRunnable(() -> scoringReportCrudService.deleteByApi(apiId))
                    .andThen(Completable.fromRunnable(() -> scoringReportCrudService.create(report)))
                    .doOnComplete(() -> asyncJobCrudService.update(job.complete()))
                    .doOnError(throwable -> log.error("Fail to save scoring report for API [{}]", apiId, throwable));
            })
            .onErrorComplete();
    }

    private ScoringReport.Summary processSummary(List<ScoringReport.Asset> assets) {
        var assetsSummary = new ArrayList<ScoringReport.Summary>();
        for (var asset : assets) {
            var counters = asset
                .diagnostics()
                .stream()
                .collect(Collectors.groupingBy(ScoringReport.Diagnostic::severity, Collectors.counting()));

            var nbErrors = counters.getOrDefault(ScoringReport.Severity.ERROR, 0L);
            var nbWarnings = counters.getOrDefault(ScoringReport.Severity.WARN, 0L);
            var nbInfos = counters.getOrDefault(ScoringReport.Severity.INFO, 0L);
            var nbHints = counters.getOrDefault(ScoringReport.Severity.HINT, 0L);

            assetsSummary.add(
                new ScoringReport.Summary(
                    scoreComputingDomainService.computeScore(nbErrors, nbWarnings, nbInfos, nbHints),
                    nbErrors,
                    nbWarnings,
                    nbInfos,
                    nbHints
                )
            );
        }

        var averageScore = BigDecimal
            .valueOf(
                assetsSummary
                    .stream()
                    .filter(summary -> summary.score() != null)
                    .mapToDouble(ScoringReport.Summary::score)
                    .average()
                    .orElse(0.0)
            )
            .setScale(2, RoundingMode.HALF_EVEN);

        return assetsSummary
            .stream()
            .reduce(
                new ScoringReport.Summary(averageScore.doubleValue(), 0L, 0L, 0L, 0L),
                (s1, s2) ->
                    new ScoringReport.Summary(
                        averageScore.doubleValue(),
                        s1.errors() + s2.errors(),
                        s1.warnings() + s2.warnings(),
                        s1.infos() + s2.infos(),
                        s1.hints() + s2.hints()
                    )
            );
    }

    public record Input(String jobId, List<ScoringReport.Asset> analyzedAssets) {}
}
