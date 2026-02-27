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
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';

import { ApiProductPlanV2Service } from '../../../../../services-ngx/api-product-plan-v2.service';
import { PlanSecurityType } from '../../../../../entities/management-api-v2';

export interface ApiProductSubscriptionTransferDialogData {
  apiProductId: string;
  currentPlanId: string;
  securityType: PlanSecurityType;
}

export interface ApiProductSubscriptionTransferDialogResult {
  selectedPlanId: string;
}

interface PlanVM {
  id: string;
  name: string;
  generalConditions: string;
}

@Component({
  selector: 'api-product-subscription-transfer-dialog',
  templateUrl: './api-product-subscription-transfer-dialog.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatRadioModule],
})
export class ApiProductSubscriptionTransferDialogComponent {
  private readonly dialogRef =
    inject<MatDialogRef<ApiProductSubscriptionTransferDialogComponent, ApiProductSubscriptionTransferDialogResult>>(MatDialogRef);
  private readonly data = inject<ApiProductSubscriptionTransferDialogData>(MAT_DIALOG_DATA);
  private readonly planService = inject(ApiProductPlanV2Service);

  protected readonly form = new UntypedFormGroup({
    selectedPlanId: new UntypedFormControl('', Validators.required),
  });

  protected readonly plans = toSignal(
    this.planService.list(this.data.apiProductId, [this.data.securityType], ['PUBLISHED'], undefined, undefined, 1, 9999).pipe(
      map(response =>
        response.data
          .filter(plan => plan.id !== this.data.currentPlanId)
          .map((plan): PlanVM => ({ id: plan.id, name: plan.name, generalConditions: plan.generalConditions })),
      ),
      catchError(() => of([] as PlanVM[])),
    ),
    { initialValue: [] as PlanVM[] },
  );

  protected get showGeneralConditionsMsg(): boolean {
    return this.plans().some(p => !!p.generalConditions);
  }

  protected onClose(): void {
    this.dialogRef.close({ selectedPlanId: this.form.getRawValue().selectedPlanId });
  }
}
