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
import { Component, HostListener, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LowerCasePipe, TitleCasePipe } from '@angular/common';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { ApiPickerComponent } from './api-picker/api-picker.component';

import { PortalNavigationApi, PortalNavigationItem, PortalNavigationItemType, PortalVisibility } from '../../../entities/management-api-v2';
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
  visibility: PortalVisibility;
  url?: string;
  apiId?: string;
  apiIds?: string[];
}

interface SectionFormControls {
  title: FormControl<string>;
  isPrivate: FormControl<boolean>;
  url?: FormControl<string>; // Optional for 'LINK' type
  apiIds?: FormControl<string[]>; // Optional for 'API' type
}

interface SectionFormValues {
  title: string;
  isPrivate: boolean;
  url?: string;
  apiIds?: string[];
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
    ApiPickerComponent,
  ],
  templateUrl: './section-editor-dialog.component.html',
  styleUrls: ['./section-editor-dialog.component.scss'],
})
export class SectionEditorDialogComponent implements OnInit {
  form!: SectionForm;
  public initialFormValues: SectionFormValues;

  public type: PortalNavigationItemType;
  public mode: SectionEditorDialogMode;
  public title: string;

  private readonly dialogRef = inject(MatDialogRef<SectionEditorDialogComponent, SectionEditorDialogResult>);
  private readonly data: SectionEditorDialogData = inject(MAT_DIALOG_DATA);
  public buttonTitle: string;

  constructor() {
    this.type = this.data.type;
    this.mode = this.data.mode;

    const typeLabel = this.type === 'API' ? 'API' : this.type.toLowerCase();

    if (this.data.mode === 'create') {
      const createLabel = this.type === 'API' ? 'APIs' : typeLabel;
      this.title = `Add ${createLabel}`;
      this.buttonTitle = 'Add';
    } else {
      this.title = `Edit "${this.data.existingItem.title}" ${typeLabel}`;
      this.buttonTitle = 'Save';
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  ngOnInit(): void {
    this.form = new FormGroup<SectionFormControls>({
      title: new FormControl<string>('', { nonNullable: true }),
      isPrivate: new FormControl(false),
    });

    this.addTypeSpecificControls();

    if (this.type !== 'API') {
      this.form.controls.title.addValidators([Validators.required]);
      this.form.controls.title.updateValueAndValidity({ emitEvent: false });
    }

    this.prefillExistingItem();

    this.initialFormValues = this.form.getRawValue();
  }

  private addTypeSpecificControls(): void {
    if (this.type === 'LINK') {
      this.form.addControl(
        'url',
        new FormControl<string>('', {
          validators: [Validators.required, urlValidator()],
          nonNullable: true,
        }),
      );
    }

    if (this.type === 'API') {
      this.form.addControl(
        'apiIds',
        new FormControl<string[]>([], {
          validators: [Validators.required],
          nonNullable: true,
        }),
      );
    }
  }

  private prefillExistingItem(): void {
    if (this.data.mode === 'edit') {
      this.form.patchValue({
        ...(this.data.existingItem.type === 'LINK' ? { url: this.data.existingItem.url } : {}),
        ...(this.data.existingItem.type === 'API' ? { apiIds: [(this.data.existingItem as PortalNavigationApi).apiId] } : {}),

        title: this.data.existingItem.title,
        isPrivate: this.data.existingItem.visibility === 'PRIVATE',
      });
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      const formValues = this.form.getRawValue();

      const apiIds = this.type === 'API' ? (formValues.apiIds ?? []) : [];
      const apiId = this.type === 'API' ? apiIds[0] : undefined;

      this.dialogRef.close({
        title: this.type === 'API' ? '' : formValues.title,
        visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
        ...(this.type === 'LINK' ? { url: formValues.url! } : {}),
        ...(this.type === 'API' ? { apiIds, apiId } : {}),
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }
}
