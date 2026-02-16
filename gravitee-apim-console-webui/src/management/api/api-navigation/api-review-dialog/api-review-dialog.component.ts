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
import { Component, Inject, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl } from '@angular/forms';
import { combineLatest, of, Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';

import { FlowService } from '../../../../services-ngx/flow.service';
import { QualityRuleService } from '../../../../services-ngx/quality-rule.service';
import { ApiQualityRuleService } from '../../../../services-ngx/api-quality-rule.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiReviewV2Service } from '../../../../services-ngx/api-review-v2.service';

export interface ApiReviewDialogData {
  apiId: string;
}
export type ApiReviewDialogResult = void;

type QualityRuleVM = {
  id: string;
  name: string;
  description: string;
  hasApiQualityRule: boolean;
  checked: boolean;
};

@Component({
  selector: 'api-review-dialog',
  templateUrl: './api-review-dialog.component.html',
  styleUrls: ['./api-review-dialog.component.scss'],
  standalone: false,
})
export class ApiReviewDialogComponent implements OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public isLoading = true;

  public reviewComments = new UntypedFormControl();

  public qualityRules: QualityRuleVM[] = [];

  constructor(
    private readonly dialogRef: MatDialogRef<ApiReviewDialogComponent, ApiReviewDialogResult>,
    @Inject(MAT_DIALOG_DATA) private readonly dialogData: ApiReviewDialogData,
    private readonly snackBarService: SnackBarService,
    private readonly qualityRuleService: QualityRuleService,
    private readonly flowService: FlowService,
    private readonly apiQualityRuleService: ApiQualityRuleService,
    private readonly apiReviewV2Service: ApiReviewV2Service,
  ) {}

  ngOnInit() {
    combineLatest([this.qualityRuleService.list(), this.apiQualityRuleService.getQualityRules(this.dialogData.apiId)])
      .pipe(
        tap(([qualityRules, apiQualityRules]) => {
          if (qualityRules.length === 0) {
            return;
          }

          this.qualityRules = qualityRules.map(qualityRule => {
            const apiQualityRule = apiQualityRules.find(qr => qr.quality_rule === qualityRule.id);
            const qualityRuleVM: QualityRuleVM = {
              id: qualityRule.id,
              name: qualityRule.name,
              description: qualityRule.description,
              hasApiQualityRule: !!apiQualityRule,
              checked: apiQualityRule ? apiQualityRule.checked : false,
            };
            return qualityRuleVM;
          });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onConfirm(accept: boolean) {
    const apiQualityRulesToSave$ = this.qualityRules.map(qualityRule => {
      if (qualityRule.hasApiQualityRule) {
        return this.apiQualityRuleService.updateQualityRule(this.dialogData.apiId, qualityRule.id, qualityRule.checked);
      } else {
        return this.apiQualityRuleService.createQualityRule(this.dialogData.apiId, qualityRule.id, qualityRule.checked);
      }
    });

    const acceptRejectToSave$ = accept
      ? this.apiReviewV2Service.accept(this.dialogData.apiId, this.reviewComments.value)
      : this.apiReviewV2Service.reject(this.dialogData.apiId, this.reviewComments.value);

    combineLatest([
      ...apiQualityRulesToSave$,
      // Default observable if no quality rules
      of({}),
    ])
      .pipe(
        switchMap(() => acceptRejectToSave$),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        () => {
          this.dialogRef.close();
          this.snackBarService.success('API review saved.');
        },
        () => {
          this.snackBarService.error('An error occurred while saving API review.');
        },
      );
  }
}
