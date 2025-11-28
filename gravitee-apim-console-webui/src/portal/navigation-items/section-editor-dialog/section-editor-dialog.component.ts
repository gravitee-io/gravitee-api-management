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
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LowerCasePipe, TitleCasePipe } from '@angular/common';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { PortalNavigationItem, PortalNavigationItemType } from '../../../entities/management-api-v2';
import { urlValidator } from '../../../shared/validators/url.validator';

export type SectionEditorDialogMode = 'create' | 'edit';

interface SectionEditorDialogCreateData {
  mode: 'create';
  type: PortalNavigationItemType;
}

interface SectionEditorDialogEditData {
  mode: 'edit';
  type: PortalNavigationItemType;
  existingItem: PortalNavigationItem;
}

export type SectionEditorDialogData = SectionEditorDialogCreateData | SectionEditorDialogEditData;

export interface SectionEditorDialogResult {
  title: string;
  url?: string;
}

interface SectionFormControls {
  title: FormControl<string>;
  url?: FormControl<string>; // Optional for 'LINK' type
}

type SectionForm = FormGroup<SectionFormControls>;

@Component({
  selector: 'section-editor-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    MatInputModule,
    TitleCasePipe,
    GioBannerModule,
    LowerCasePipe,
  ],
  templateUrl: './section-editor-dialog.component.html',
  styleUrls: ['./section-editor-dialog.component.scss'],
})
export class SectionEditorDialogComponent implements OnInit {
  form: SectionForm = new FormGroup<SectionFormControls>({
    title: new FormControl<string>('', { validators: [Validators.required], nonNullable: true }),
  });

  public type: PortalNavigationItemType;
  public mode: SectionEditorDialogMode;
  public title: string;

  private readonly dialogRef = inject(MatDialogRef<SectionEditorDialogComponent, SectionEditorDialogResult>);
  private readonly data: SectionEditorDialogData = inject(MAT_DIALOG_DATA);
  public buttonTitle: string;

  constructor() {
    this.type = this.data.type;
    this.mode = this.data.mode;
    if (this.data.mode === 'create') {
      this.title = `Add ${this.type.toLowerCase()}`;
      this.buttonTitle = 'Add';
    } else {
      this.title = `Edit "${this.data.existingItem.title}" ${this.type.toLowerCase()}`;
      this.buttonTitle = 'Save';
    }
  }

  ngOnInit(): void {
    this.addTypeSpecificControls();
    this.prefillExistingItem();
  }

  private addTypeSpecificControls(): void {
    if (this.type === 'LINK') {
      this.form.addControl('url', new FormControl<string>('', { validators: [Validators.required, urlValidator()], nonNullable: true }));
    }
  }

  private prefillExistingItem(): void {
    if (this.data.mode === 'edit') {
      this.form.patchValue({
        ...(this.data.existingItem.type === 'LINK' ? { url: this.data.existingItem.url } : {}),

        title: this.data.existingItem.title,
      });
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close({ ...this.form.getRawValue() });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
