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
import io.gravitee.apim.core.integration.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.scoring.crud_service.ScoringReportCrudService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class SaveScoringResponseUseCase {

    private final AsyncJobCrudService asyncJobCrudService;
    private final ScoringReportCrudService scoringReportCrudService;

    public SaveScoringResponseUseCase(AsyncJobCrudService asyncJobCrudService, ScoringReportCrudService scoringReportCrudService) {
        this.asyncJobCrudService = asyncJobCrudService;
        this.scoringReportCrudService = scoringReportCrudService;
    }

    public Completable execute(Input input) {
        return Maybe
            .defer(() -> Maybe.fromOptional(asyncJobCrudService.findById(input.jobId)))
            .subscribeOn(Schedulers.computation())
            .flatMapCompletable(job -> {
                String apiId = job.getSourceId();

                var report = ScoringReport
                    .builder()
                    .id(job.getId())
                    .apiId(apiId)
                    .createdAt(TimeProvider.now())
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

    public record Input(String jobId, List<ScoringReport.Asset> analyzedAssets) {}
}
