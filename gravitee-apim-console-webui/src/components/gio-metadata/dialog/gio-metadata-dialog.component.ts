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
import { AfterViewChecked, ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { distinctUntilChanged, takeUntil, tap } from 'rxjs/operators';

import { MetadataFormat } from '../../../entities/metadata/metadata';
import { MAIL_PATTERN, URL_PATTERN } from '../../../shared/utils';

export interface GioMetadataDialogData {
  key?: string;
  name?: string;
  format?: MetadataFormat;
  value?: any;
  defaultValue?: string;
  referenceType: 'API' | 'Application' | 'Global';
  action: 'Update' | 'Create';
  readOnly?: boolean;
}

@Component({
  selector: 'gio-metadata-dialog',
  templateUrl: './gio-metadata-dialog.component.html',
  styleUrls: ['./gio-metadata-dialog.component.scss'],
  standalone: false,
})
export class GioMetadataDialogComponent implements OnInit, AfterViewChecked {
  private unsubscribe$: Subject<void> = new Subject<void>();

  metadata: GioMetadataDialogData;
  hasChange: boolean;
  form: UntypedFormGroup;
  formats: string[];
  mailPattern = MAIL_PATTERN;
  urlPattern = URL_PATTERN;

  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly dialogRef: MatDialogRef<GioMetadataDialogData, GioMetadataDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: GioMetadataDialogData,
    private readonly formBuilder: UntypedFormBuilder,
  ) {
    this.metadata = dialogData;
  }

  ngOnInit() {
    this.formats = ['STRING', 'NUMERIC', 'BOOLEAN', 'DATE', 'MAIL', 'URL'];

    if (this.metadata?.format === 'BOOLEAN') {
      this.metadata.value = this.metadata.value === 'true';
    }

    this.form = this.formBuilder.group({
      key: this.formBuilder.control({ value: this.metadata?.key, disabled: true }),
      name: this.formBuilder.control(this.metadata?.name, [Validators.required]),
      format: this.formBuilder.control({ value: this.metadata?.format, disabled: this.metadata.action === 'Update' }, [
        Validators.required,
      ]),
      value: this.formBuilder.control(this.metadata?.value, [Validators.required]),
      defaultValue: this.formBuilder.control({ value: this.metadata?.defaultValue, disabled: true }),
    });

    if (this.metadata.readOnly) {
      this.form.disable({ emitEvent: false });
    }

    const initialFormValue = this.form.value;
    this.form.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
      this.hasChange = Object.keys(initialFormValue).some((key) => value[key] !== initialFormValue[key]);
    });

    this.form
      .get('format')
      .valueChanges.pipe(
        distinctUntilChanged(),
        tap((val: MetadataFormat) => {
          if (val === 'BOOLEAN') {
            this.form.get('value').setValue(false); // set default to false
          } else {
            this.form.get('value').reset();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngAfterViewChecked() {
    this.changeDetectorRef.detectChanges();
  }

  onClose() {
    this.dialogRef.close();
  }

  save() {
    const metadataToSave: GioMetadataDialogData = { ...this.metadata, ...this.form.value };
    this.dialogRef.close(metadataToSave);
  }
}
