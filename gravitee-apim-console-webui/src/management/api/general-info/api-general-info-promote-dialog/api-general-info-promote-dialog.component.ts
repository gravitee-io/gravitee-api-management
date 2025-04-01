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
import { UntypedFormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';

import { ApiService } from '../../../../services-ngx/api.service';
import { CockpitService, UtmCampaign } from '../../../../services-ngx/cockpit.service';
import { PromotionService } from '../../../../services-ngx/promotion.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2 } from '../../../../entities/management-api-v2';

export type ApiPortalDetailsPromoteDialogData = {
  api: ApiV2;
};

@Component({
  selector: 'api-general-info-promote-dialog',
  templateUrl: './api-general-info-promote-dialog.component.html',
  styleUrls: ['./api-general-info-promote-dialog.component.scss'],
  standalone: false,
})
export class ApiGeneralInfoPromoteDialogComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public state: 'loading' | 'hasCockpit' | 'meetCockpit' = 'loading';

  public apiId: string;
  public promotionTargets: {
    id: string;
    name: string;
    promotionInProgress: boolean;
  }[] = [];
  public hasPromotionInProgress = false;
  public cockpitURL: string;

  public promoteControl = new UntypedFormControl('', [Validators.required]);

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalDetailsPromoteDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalDetailsPromoteDialogData,
    private readonly apiService: ApiService,
    private readonly promotionService: PromotionService,
    private readonly snackBarService: SnackBarService,
    private readonly cockpitService: CockpitService,
  ) {
    this.apiId = dialogData.api.id;

    combineLatest([
      this.promotionService.listPromotionTargets(),
      this.promotionService.listPromotion({
        apiId: this.apiId,
        statuses: ['CREATED', 'TO_BE_VALIDATED'],
      }),
    ])
      .pipe(
        map(([targetEnvs, promotions]) => {
          this.promotionTargets = targetEnvs
            .map((promotionTarget) => ({
              id: promotionTarget.id,
              name: promotionTarget.name,
              promotionInProgress: promotions.some((promotion) => promotion.targetEnvCockpitId === promotionTarget.id),
            }))
            .sort((target1, target2) => target1.name.localeCompare(target2.name));

          this.hasPromotionInProgress = this.promotionTargets.some((target) => target.promotionInProgress);

          this.promoteControl.setValue(this.promotionTargets.find((target) => !target.promotionInProgress)?.id);

          this.state = 'hasCockpit';
        }),
        catchError((error) => {
          if (error.error?.technicalCode === 'installation.notAccepted') {
            const { cockpitURL } = error.error.parameters;
            this.cockpitURL = this.cockpitService.addQueryParamsForAnalytics(cockpitURL, UtmCampaign.API_PROMOTION);
            this.state = 'meetCockpit';
            return EMPTY;
          }

          this.snackBarService.error(error.error?.message ?? error.message ?? 'An error occurred while loading promotion targets.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onPromote() {
    const promotionTarget = this.promotionTargets.find((target) => target.id === this.promoteControl.value);

    this.promotionService
      .promote(this.apiId, promotionTarget)
      .pipe(
        tap(() => {
          this.snackBarService.success('Promotion requested.');
        }),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? error.message ?? 'An error occurred while requesting promotion.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.dialogRef.close());
  }
}
