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
import { GioBannerModule, GioFormSelectionInlineModule } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import {
  PortalNavigationItem,
  PortalNavigationItemType,
  PortalNavigationLink,
  PortalPageContentType,
  PortalVisibility,
} from '../../../entities/management-api-v2';
import { urlValidator } from '../../../shared/validators/url.validator';

export type SectionEditorDialogMode = 'create' | 'edit';

export type SectionEditorDialogItemType = Exclude<PortalNavigationItemType, 'API'>;

interface SectionEditorDialogCreateData {
  mode: 'create';
  type: SectionEditorDialogItemType;
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
  contentType?: PortalPageContentType;
}

export interface PortalPageTypeOption {
  value: PortalPageContentType;
  label: string;
  icon: string;
  available: boolean;
}

export const PORTAL_PAGE_CONTENT_TYPE_OPTIONS: PortalPageTypeOption[] = [
  { value: 'GRAVITEE_MARKDOWN', label: 'Markdown', icon: 'gio:gravitee', available: true },
  { value: 'OPENAPI', label: 'OpenAPI', icon: 'gio:open-api', available: true },
];

interface SectionFormControls {
  title: FormControl<string>;
  isPrivate: FormControl<boolean>;
  url?: FormControl<string>; // Optional for 'LINK' type
  contentType?: FormControl<PortalPageContentType>; // Optional for 'PAGE' create only
}

interface SectionFormValues {
  title: string;
  isPrivate: boolean;
  url?: string;
  contentType?: PortalPageContentType;
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
    GioFormSelectionInlineModule,
    LowerCasePipe,
  ],
  templateUrl: './section-editor-dialog.component.html',
  styleUrls: ['./section-editor-dialog.component.scss'],
})
export class SectionEditorDialogComponent implements OnInit {
  form: SectionForm = new FormGroup<SectionFormControls>({
    title: new FormControl<string>('', { validators: [Validators.required], nonNullable: true }),
    isPrivate: new FormControl(false),
  });
  public initialFormValues: SectionFormValues;

  public type: PortalNavigationItemType;
  public mode: SectionEditorDialogMode;
  public title: string;
  readonly pageContentTypeOptions = PORTAL_PAGE_CONTENT_TYPE_OPTIONS;

  showPageTypeSelection(): boolean {
    return this.mode === 'create' && this.type === 'PAGE';
  }

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

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  ngOnInit(): void {
    this.addTypeSpecificControls();
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
    if (this.showPageTypeSelection()) {
      this.form.addControl('contentType', new FormControl<PortalPageContentType>('GRAVITEE_MARKDOWN', { nonNullable: true }));
    }
  }

  private prefillExistingItem(): void {
    if (this.data.mode === 'edit') {
      this.form.patchValue({
        ...(this.data.existingItem.type === 'LINK' ? { url: (this.data.existingItem as PortalNavigationLink).url } : {}),
        title: this.data.existingItem.title,
        isPrivate: this.data.existingItem.visibility === 'PRIVATE',
      });
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      const formValues = this.form.getRawValue();

      this.dialogRef.close({
        title: formValues.title,
        visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
        ...(this.type === 'LINK' ? { url: formValues.url! } : {}),
        ...(this.showPageTypeSelection() && formValues.contentType ? { contentType: formValues.contentType } : {}),
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
