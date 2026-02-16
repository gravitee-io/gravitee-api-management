/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, Observable, Subject, timer } from 'rxjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { ApiScoring, ScoringAsset, ScoringDiagnostic, ScoringError, ScoringSeverity } from './api-scoring.model';

import { ApiScoringService } from '../../../services-ngx/api-scoring.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api } from '../../../entities/management-api-v2';
import { AsyncJobService } from '../../../services-ngx/async-job.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface ScoreEvaluationErrors {
  assetName?: string;
  errors: ScoringError[];
}

@Component({
  selector: 'app-api-scoring',
  templateUrl: './api-scoring.component.html',
  styleUrl: './api-scoring.component.scss',
  standalone: false,
})
export class ApiScoringComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  private apiId = this.activatedRoute.snapshot.params.apiId;
  private stopPolling$ = new Subject<void>();
  private allApiScoring: ApiScoring;

  public apiScoring: ApiScoring;
  public status: ScoringSeverity | 'ALL' = 'ALL';
  public isLoading = true;
  public api: Api;
  public pendingScoreRequest = false;
  protected readonly ScoringSeverity = ScoringSeverity;

  public apiScoreNeverEvaluated = false;
  public apiScoreAvailable = false;
  public evaluationErrors: ScoreEvaluationErrors[] = [];

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiScoringService: ApiScoringService,
    private readonly asyncJobService: AsyncJobService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.initialize();
  }

  public evaluate() {
    this.apiScoringService
      .evaluate(this.api.id)
      .pipe(
        tap(() => {
          this.pendingScoreRequest = true;
          this.initialize();
        }),
      )
      .subscribe();
  }

  public isScoringRequestPending(): Observable<boolean> {
    return timer(0, 1000).pipe(
      switchMap(() =>
        this.asyncJobService.listAsyncJobs({
          type: 'SCORING_REQUEST',
          sourceId: this.apiId,
        }),
      ),
      map(response => {
        response?.data?.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
        const lastJob = response?.data?.[0];
        if (lastJob?.updatedAt && 3_600_000 > new Date().getTime() - new Date(lastJob?.updatedAt).getTime()) {
          if (lastJob?.status === 'ERROR') {
            this.snackBarService.error(`The last evaluation was failed at ${lastJob.createdAt} ${lastJob.errorMessage ?? ''}`);
          } else if (lastJob?.status === 'TIMEOUT') {
            this.snackBarService.error(
              `Evaluation timed out at ${lastJob.createdAt}. The API score service might be unavailable. Please try again later.`,
            );
          }
        }
        return lastJob?.status === 'PENDING';
      }),
      takeUntil(this.stopPolling$),
    );
  }

  private initialize() {
    this.isScoringRequestPending()
      .pipe(
        switchMap(isPending => {
          return combineLatest([this.apiService.get(this.apiId), this.apiScoringService.getApiScoring(this.apiId)]).pipe(
            map(([api, apiScoring]) => {
              return {
                pendingScoreRequest: isPending,
                api,
                apiScoring,
              };
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ pendingScoreRequest, api, apiScoring }) => {
          this.isLoading = false;

          this.pendingScoreRequest = pendingScoreRequest;
          this.api = api;
          this.allApiScoring = apiScoring;
          this.apiScoring = apiScoring;
          this.filterScoringAssets('ALL');

          if (!this.pendingScoreRequest) {
            this.stopPolling$.next();

            this.apiScoreNeverEvaluated = apiScoring === undefined;
            if (!this.apiScoreNeverEvaluated) {
              this.evaluationErrors = this.getEvaluationErrors(apiScoring);
              if (this.evaluationErrors.length) {
                this.snackBarService.error(this.formatEvaluationErrors(this.evaluationErrors));
              }
              this.apiScoreAvailable = this.apiScoring.summary !== undefined;
            }
          }
        },
        error: e => {
          this.isLoading = false;
          this.snackBarService.error(e.error?.message ?? 'An error occurred while getting your API Scoring.');
        },
      });
  }

  public filterScoringAssets(severity: ScoringSeverity | 'ALL') {
    this.status = severity;

    if (severity === 'ALL') {
      this.apiScoring = this.allApiScoring;
      return;
    }

    const filteredScoringAssets: ScoringAsset[] = this.allApiScoring.assets.map(
      (asset: ScoringAsset): ScoringAsset => ({
        ...asset,
        diagnostics: asset.diagnostics.filter((diagnostic: ScoringDiagnostic) => diagnostic.severity === severity),
      }),
    );

    this.apiScoring = {
      ...this.apiScoring,
      assets: filteredScoringAssets,
    };
  }

  private getEvaluationErrors(apiScoring: ApiScoring): ScoreEvaluationErrors[] {
    return apiScoring.assets
      .filter(asset => asset?.errors?.length > 0)
      .map(asset => ({
        assetName: asset.name,
        errors: asset.errors,
      }));
  }

  private formatEvaluationErrors(evaluationErrors: ScoreEvaluationErrors[]): string {
    let errorMessage = 'Errors occurred while scoring this API:';
    evaluationErrors.forEach(asset => {
      errorMessage += '\n';
      if (asset.assetName) {
        errorMessage += '\nAsset: ' + asset.assetName;
      }
      asset.errors.forEach(error => {
        errorMessage += '\nCode: ' + error.code + '\n' + 'Path: ' + error.path.toString();
      });
    });
    return errorMessage;
  }
}
