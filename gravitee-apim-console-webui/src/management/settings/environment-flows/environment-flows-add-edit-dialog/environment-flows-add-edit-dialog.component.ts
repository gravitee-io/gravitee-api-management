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
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { CommonModule } from '@angular/common';
import { GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { map, startWith } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ApiV4, EnvironmentFlow, ExecutionPhase } from '../../../../entities/management-api-v2';

export type EnvironmentFlowsAddEditDialogData =
  | {
      apiType: ApiV4['type'];
    }
  | {
      environmentFlow: EnvironmentFlow;
    };

export type EnvironmentFlowsAddEditDialogResult = undefined | { name: string; description?: string; phase: ExecutionPhase };

const PHASE_BY_API_TYPE: Record<ApiV4['type'], ExecutionPhase[]> = {
  PROXY: ['REQUEST', 'RESPONSE'],
  MESSAGE: ['REQUEST', 'RESPONSE', 'MESSAGE_REQUEST', 'MESSAGE_RESPONSE'],
};

@Component({
  selector: 'environment-flows-add-edit-dialog',
  templateUrl: './environment-flows-add-edit-dialog.component.html',
  styleUrls: ['./environment-flows-add-edit-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormField,
    MatInput,
    MatLabel,
    MatButtonToggleModule,
    ReactiveFormsModule,
    GioFormFocusInvalidModule,
  ],
})
export class EnvironmentFlowsAddEditDialogComponent {
  protected apiTypeLabel: string;

  protected formGroup: FormGroup<{ name: FormControl<string>; description: FormControl<string>; phase: FormControl<ExecutionPhase> }>;
  protected isValid$: Observable<boolean>;

  protected phases: ExecutionPhase[];

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: EnvironmentFlowsAddEditDialogData,
    public dialogRef: MatDialogRef<EnvironmentFlowsAddEditDialogComponent, EnvironmentFlowsAddEditDialogResult>,
  ) {
    const apiType = isEdit(data) ? data.environmentFlow.apiType : data.apiType;

    this.apiTypeLabel = apiType === 'MESSAGE' ? 'Message' : 'Proxy';
    this.phases = PHASE_BY_API_TYPE[apiType];

    this.formGroup = new FormGroup({
      name: new FormControl(isEdit(data) ? data.environmentFlow.name : '', Validators.required),
      description: new FormControl(isEdit(data) ? data.environmentFlow.description : ''),
      phase: new FormControl(isEdit(data) ? { disabled: true, value: data.environmentFlow.phase } : this.phases[0], Validators.required),
    });

    this.isValid$ = this.formGroup.statusChanges.pipe(
      startWith(this.formGroup.status),
      map((status) => status === 'VALID'),
    );
  }

  protected onSave(): void {
    if (this.formGroup.invalid) {
      return;
    }
    this.dialogRef.close({
      name: this.formGroup.get('name').value,
      description: this.formGroup.get('description').value,
      phase: this.formGroup.get('phase').value,
    });
  }
}

const isEdit = (data: EnvironmentFlowsAddEditDialogData): data is { environmentFlow: EnvironmentFlow } => 'environmentFlow' in data;
