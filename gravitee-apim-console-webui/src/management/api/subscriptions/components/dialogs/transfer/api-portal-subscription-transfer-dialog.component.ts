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
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { map, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ApiPlanV2Service } from '../../../../../../services-ngx/api-plan-v2.service';
import { PlanMode, PlanSecurityType } from '../../../../../../entities/management-api-v2';

export interface ApiPortalSubscriptionTransferDialogData {
  apiId: string;
  currentPlanId: string;
  securityType: PlanSecurityType;
  mode: PlanMode;
}

export interface ApiPortalSubscriptionTransferDialogResult {
  selectedPlanId: string;
}

interface PlanVM {
  id: string;
  name: string;
  generalConditions: string;
}
@Component({
  selector: 'api-portal-subscription-transfer',
  templateUrl: './api-portal-subscription-transfer-dialog.component.html',
  styleUrls: ['./api-portal-subscription-transfer-dialog.component.scss'],
  standalone: false,
})
export class ApiPortalSubscriptionTransferDialogComponent implements OnInit {
  plans: PlanVM[];
  showGeneralConditionsMsg: boolean;
  form: UntypedFormGroup;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private data: ApiPortalSubscriptionTransferDialogData;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiPortalSubscriptionTransferDialogComponent, ApiPortalSubscriptionTransferDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiPortalSubscriptionTransferDialogData,
    private readonly apiPlanService: ApiPlanV2Service,
  ) {
    this.data = dialogData;
  }

  ngOnInit(): void {
    this.form = new UntypedFormGroup({ selectedPlanId: new UntypedFormControl('', { validators: Validators.required }) });

    this.apiPlanService
      .list(this.data.apiId, [this.data.securityType], ['PUBLISHED'], this.data.mode, undefined, 1, 9999)
      .pipe(
        map(response => response.data.filter(plan => plan.id !== this.data.currentPlanId)),

        takeUntil(this.unsubscribe$),
      )
      .subscribe(plans => {
        this.plans = plans.map(plan => ({ id: plan.id, name: plan.name, generalConditions: plan.generalConditions }));
        this.showGeneralConditionsMsg = this.plans.some(info => info.generalConditions);
      });
  }

  onClose() {
    this.dialogRef.close({
      selectedPlanId: this.form.getRawValue().selectedPlanId,
    });
  }
}
