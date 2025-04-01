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

import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { forEach } from 'lodash';
import { combineLatest, Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { ApiQualityMetrics } from '../../../../entities/api';
import { ApiService } from '../../../../services-ngx/api.service';
import { QualityRuleService } from '../../../../services-ngx/quality-rule.service';

@Component({
  selector: 'api-general-info-quality',
  templateUrl: './api-general-info-quality.component.html',
  styleUrls: ['./api-general-info-quality.component.scss'],
  standalone: false,
})
export class ApiGeneralInfoQualityComponent implements OnChanges, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input()
  public apiId: string;

  public isQualityEnabled = false;
  public qualityMetricsDescription: Record<string, string> = {
    'api.quality.metrics.functional.documentation.weight': 'A functional page must be published',
    'api.quality.metrics.technical.documentation.weight': 'A swagger page must be published',
    'api.quality.metrics.healthcheck.weight': 'An healthcheck must be configured',
    'api.quality.metrics.description.weight': 'The API description must be filled',
    'api.quality.metrics.logo.weight': 'Put your own logo',
    'api.quality.metrics.categories.weight': 'Link your API to categories',
    'api.quality.metrics.labels.weight': 'Add labels to your API',
  };
  public qualityMetrics: ApiQualityMetrics;

  constructor(
    private readonly apiService: ApiService,
    private readonly qualityRuleService: QualityRuleService,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.apiId) {
      combineLatest([this.qualityRuleService.list(), this.apiService.getQualityMetrics(this.apiId)])
        .pipe(
          tap(([qualityRules, qualityMetrics]) => {
            forEach(qualityRules, (qualityRule) => {
              this.qualityMetricsDescription[qualityRule.id] = qualityRule.description;
            });
            this.qualityMetrics = qualityMetrics;
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
