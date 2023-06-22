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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { map, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ApiPlanV2Service } from '../../../../../../services-ngx/api-plan-v2.service';
import { PlanMode, PlanSecurityType } from '../../../../../../entities/management-api-v2';

export interface SubscriptionTransferData {
  apiId: string;
  currentPlanId: string;
  securityType: PlanSecurityType;
  mode: PlanMode;
}
interface PlanVM {
  id: string;
  name: string;
  generalConditions: string;
}
@Component({
  selector: 'api-portal-subscription-transfer',
  template: require('./api-portal-subscription-transfer-dialog.component.html'),
  styles: [require('./api-portal-subscription-transfer-dialog.component.scss')],
})
export class ApiPortalSubscriptionTransferDialogComponent implements OnInit {
  plans: PlanVM[];
  showGeneralConditionsMsg: boolean;
  form: FormGroup;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private data: SubscriptionTransferData;

  constructor(
    private readonly dialogRef: MatDialogRef<string>,
    @Inject(MAT_DIALOG_DATA) dialogData: SubscriptionTransferData,
    private readonly apiPlanService: ApiPlanV2Service,
  ) {
    this.data = dialogData;
  }

  ngOnInit(): void {
    this.form = new FormGroup({ selectedPlanId: new FormControl('', { validators: Validators.required }) });

    this.apiPlanService
      .list(this.data.apiId, [this.data.securityType], ['PUBLISHED'], this.data.mode, 1, 9999)
      .pipe(
        map((response) => response.data.filter((plan) => plan.id !== this.data.currentPlanId)),

        takeUntil(this.unsubscribe$),
      )
      .subscribe((plans) => {
        this.plans = plans.map((plan) => ({ id: plan.id, name: plan.name, generalConditions: plan.generalConditions }));
        this.showGeneralConditionsMsg = this.plans.some((info) => info.generalConditions);
      });
  }

  onClose() {
    this.dialogRef.close(this.form.getRawValue().selectedPlanId);
  }
}
