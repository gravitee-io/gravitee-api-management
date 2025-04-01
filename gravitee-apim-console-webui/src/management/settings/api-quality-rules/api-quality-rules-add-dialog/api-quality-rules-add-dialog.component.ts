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
import { FormControl, FormGroup, UntypedFormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ApiQualityRulesDialogData {
  weight: number;
  name: string;
  description: string;
  id?: string;
}

export interface ApiQualityRulesDialogResult {
  weight: number;
  name: string;
  description: string;
}

interface NewQualityRuleForm {
  name: FormControl<string>;
  description: FormControl<string>;
  weight: FormControl<number>;
}

@Component({
  selector: 'api-quality-rules-add-dialog',
  templateUrl: './api-quality-rules-add-dialog.component.html',
  styleUrls: ['./api-quality-rules-add-dialog.component.scss'],
  standalone: false,
})
export class ApiQualityRulesAddDialogComponent implements OnInit {
  newQualityRuleForm: UntypedFormGroup;
  qualityRoleForm: ApiQualityRulesDialogData;

  constructor(
    private readonly dialogRef: MatDialogRef<ApiQualityRulesDialogData, ApiQualityRulesDialogResult>,
    @Inject(MAT_DIALOG_DATA) dialogData: ApiQualityRulesDialogData,
  ) {
    this.qualityRoleForm = dialogData;
  }

  ngOnInit() {
    this.newQualityRuleForm = new FormGroup<NewQualityRuleForm>({
      name: new FormControl(this.qualityRoleForm?.name),
      description: new FormControl(this.qualityRoleForm?.description),
      weight: new FormControl(this.qualityRoleForm?.weight),
    });
  }

  onSubmit() {
    const { name, description, weight } = this.newQualityRuleForm.getRawValue();
    this.dialogRef.close({
      name,
      description,
      weight,
    });
  }
}
