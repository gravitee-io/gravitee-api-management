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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { CommonModule } from '@angular/common';
import { GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';
import { map, startWith } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { ApiV4, SharedPolicyGroup, ExecutionPhase, toReadableExecutionPhase } from '../../../../entities/management-api-v2';

export type SharedPolicyGroupAddEditDialogData =
  | {
      apiType: ApiV4['type'];
    }
  | {
      sharedPolicyGroup: SharedPolicyGroup;
    };

export type SharedPolicyGroupAddEditDialogResult =
  | undefined
  | { name: string; description?: string; prerequisiteMessage?: string; phase: ExecutionPhase };

const PHASE_BY_API_TYPE: Record<ApiV4['type'], ExecutionPhase[]> = {
  PROXY: ['REQUEST', 'RESPONSE'],
  MESSAGE: ['REQUEST', 'RESPONSE', 'MESSAGE_REQUEST', 'MESSAGE_RESPONSE'],
};

@Component({
  selector: 'shared-policy-groups-add-edit-dialog',
  templateUrl: './shared-policy-groups-add-edit-dialog.component.html',
  styleUrls: ['./shared-policy-groups-add-edit-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInput,
    MatButtonToggleModule,
    GioFormFocusInvalidModule,
  ],
})
export class SharedPolicyGroupsAddEditDialogComponent {
  protected apiTypeLabel: string;

  protected formGroup: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    prerequisiteMessage: FormControl<string>;
    phase: FormControl<ExecutionPhase>;
  }>;
  protected isValid$: Observable<boolean>;

  protected phases: { name: string; value: ExecutionPhase }[];
  protected isEdit: boolean;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: SharedPolicyGroupAddEditDialogData,
    public dialogRef: MatDialogRef<SharedPolicyGroupsAddEditDialogComponent, SharedPolicyGroupAddEditDialogResult>,
  ) {
    this.isEdit = isEdit(data);
    const apiType = isEdit(data) ? data.sharedPolicyGroup.apiType : data.apiType;

    this.apiTypeLabel = apiType === 'MESSAGE' ? 'Message' : 'Proxy';
    this.phases = PHASE_BY_API_TYPE[apiType].map((phase) => ({ name: toReadableExecutionPhase(phase), value: phase }));

    this.formGroup = new FormGroup({
      name: new FormControl(isEdit(data) ? data.sharedPolicyGroup.name : '', Validators.required),
      description: new FormControl(isEdit(data) ? data.sharedPolicyGroup.description : ''),
      prerequisiteMessage: new FormControl(isEdit(data) ? data.sharedPolicyGroup.prerequisiteMessage : ''),
      phase: new FormControl(
        isEdit(data) ? { disabled: true, value: data.sharedPolicyGroup.phase } : this.phases[0].value,
        Validators.required,
      ),
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
      prerequisiteMessage: this.formGroup.get('prerequisiteMessage').value,
      phase: this.formGroup.get('phase').value,
    });
  }
}

const isEdit = (data: SharedPolicyGroupAddEditDialogData): data is { sharedPolicyGroup: SharedPolicyGroup } => 'sharedPolicyGroup' in data;
