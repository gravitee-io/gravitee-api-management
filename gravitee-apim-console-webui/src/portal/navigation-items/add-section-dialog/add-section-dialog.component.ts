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
import { Component, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { PortalNavigationItemType } from '../../../entities/management-api-v2';

export interface AddSectionDialogData {
  type: PortalNavigationItemType;
}

export interface AddSectionDialogResult {
  id: string;
}

interface BaseForm {
  title: FormControl<string>;
  isPublic: FormControl<boolean>;
}

interface PageForm extends BaseForm {}

interface FolderForm extends BaseForm {}

interface LinkForm extends BaseForm {
  settings: FormGroup<{
    url: FormControl<string | null>;
  }>;
}

type SectionForm = FormGroup<BaseForm | PageForm | FolderForm | LinkForm>;

@Component({
  selector: 'add-section-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
  ],
  templateUrl: './add-section-dialog.component.html',
  styleUrl: './add-section-dialog.component.scss',
})
export class AddSectionDialogComponent implements OnInit {
  form: SectionForm = new FormGroup<BaseForm>({
    title: new FormControl<string>('', { validators: [Validators.required], nonNullable: true }),
    isPublic: new FormControl<boolean>(true, { nonNullable: true }),
  });

  private readonly dialogRef = inject(MatDialogRef<AddSectionDialogData, AddSectionDialogResult>);
  private readonly type: PortalNavigationItemType = inject<AddSectionDialogData>(MAT_DIALOG_DATA).type;

  constructor() {
    // this.type = this.data.type;
    // this.form = this.createForm();
  }

  ngOnInit(): void {
    this.updateFormForType();
  }

  private createForm(): SectionForm {
    const baseForm = new FormGroup<BaseForm>({
      title: new FormControl<string>('', { validators: [Validators.required], nonNullable: true }),
      isPublic: new FormControl<boolean>(true, { nonNullable: true }),
    });

    return baseForm as SectionForm;
  }

  private updateFormForType(): void {
    // Remove type-specific fields first
    this.removeTypeSpecificFields();

    // Add type-specific fields and validators
    switch (this.type) {
      case 'PAGE':
        this.addPageFields();
        break;
      case 'LINK':
        this.addLinkFields();
        break;
      case 'FOLDER':
        // No additional fields needed for folder
        break;
    }
  }

  private removeTypeSpecificFields(): void {
    if (this.form.contains('settings')) {
      (this.form as any).removeControl('settings');
    }
  }

  private addPageFields(): void {
    // No additional fields needed for page
  }

  private addLinkFields(): void {
    const linkForm = this.form as FormGroup<LinkForm>;
    if (!linkForm.contains('settings')) {
      linkForm.addControl(
        'settings',
        new FormGroup({
          url: new FormControl<string | null>(null, { validators: [Validators.required] }),
        }),
      );
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      const formValue = this.form.getRawValue();
      // The actual submission logic will be implemented when the service is integrated
      // For now, we'll just close with a mock result
      this.dialogRef.close({ id: 'mock-id' });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  // get isPageType(): boolean {
  //   return this.type === 'PAGE';
  // }
  //
  // get isLinkType(): boolean {
  //   return this.type === 'LINK';
  // }
  //
  // get isFolderType(): boolean {
  //   return this.type === 'FOLDER';
  // }
}
