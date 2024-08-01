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
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

import { CustomUserField } from '../../../../../entities/customUserFields';

export interface CustomUserFieldsDialogData {
  action: 'Update' | 'Create';
  key?: string;
  label?: string;
  required?: boolean;
  values?: string[];
}

export type CustomUserFieldsDialogResult = CustomUserField;

@Component({
  selector: 'app-custom-user-fields-dialog',
  templateUrl: './custom-user-fields-dialog.component.html',
  styleUrl: './custom-user-fields-dialog.component.scss',
})
export class CustomUserFieldsDialogComponent implements OnInit {
  public customUserFieldsDialogData: CustomUserFieldsDialogData;
  public form;
  public isUpdate = false;
  private options = ['address', 'city', 'country', 'job_position', 'organization', 'telephone_number', 'zip_code'];
  public filteredOptions: Observable<string[]>;

  constructor(
    private readonly dialogRef: MatDialogRef<CustomUserFieldsDialogData, CustomUserFieldsDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: CustomUserFieldsDialogData,
    private readonly formBuilder: UntypedFormBuilder,
  ) {
    this.customUserFieldsDialogData = dialogData;
  }

  ngOnInit() {
    this.isUpdate = this.customUserFieldsDialogData.action === 'Update';
    this.buildForm();
  }

  private regEx = /^[a-zA-Z0-9_\\-]+$/;

  private buildForm() {
    this.form = this.formBuilder.group({
      key: [
        {
          value: '',
          disabled: this.isUpdate,
        },
        [Validators.required, Validators.minLength(1), Validators.maxLength(50), Validators.pattern(this.regEx)],
      ],
      label: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
      required: [false],
      values: [[]],
    });
    if (this.isUpdate) {
      this.seedData();
    }
  }

  private seedData() {
    const { key, label, required, values } = this.customUserFieldsDialogData;

    this.form.patchValue({
      key,
      label,
      required,
      values,
    });
  }

  public initOptions() {
    this.filteredOptions = this.form.controls.key.valueChanges.pipe(
      startWith(''),
      map((value) => this._filter(value || '')),
    );
  }

  private _filter(value: any): string[] {
    const filterValue = value.toLowerCase();
    return this.options.filter((option) => option.toLowerCase().includes(filterValue));
  }

  public onClose() {
    this.dialogRef.close();
  }

  public save() {
    this.dialogRef.close({ key: this.customUserFieldsDialogData.key, ...this.form.value });
  }
}
