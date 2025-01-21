/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, inject, Inject } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS, MatOption, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';

import { ApplicationTabLogsService, HttpStatusVM } from '../application-tab-logs.service';

export interface MoreFiltersDialogData {
  startDate?: number;
  endDate?: number;
  requestId?: string;
  transactionId?: string;
  httpStatuses?: HttpStatusVM[];
  messageText?: string;
  path?: string;
}

@Component({
  imports: [
    MatDatepickerModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    ReactiveFormsModule,
    FormsModule,
    MatSelect,
    MatOption,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS }],
  selector: 'app-more-filters-dialog',
  styleUrl: './more-filters-dialog.component.scss',
  templateUrl: './more-filters-dialog.component.html',
})
export class MoreFiltersDialogComponent {
  moreFiltersForm: FormGroup<{
    startDate: FormControl<Date | null>;
    endDate: FormControl<Date | null>;
    requestId: FormControl<string | null>;
    transactionId: FormControl<string | null>;
    httpStatuses: FormControl<string[] | null>;
    messageText: FormControl<string | null>;
    path: FormControl<string | null>;
  }>;

  httpStatusChoices = inject(ApplicationTabLogsService).httpStatuses;

  private readonly initialData: MoreFiltersDialogData;

  constructor(
    private readonly dialogRef: MatDialogRef<MoreFiltersDialogComponent, MoreFiltersDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: MoreFiltersDialogData,
  ) {
    this.initialData = dialogData;

    this.moreFiltersForm = new FormGroup({
      startDate: new FormControl<Date | null>(dialogData.startDate ? new Date(dialogData.startDate) : null),
      endDate: new FormControl<Date | null>(dialogData.endDate ? new Date(dialogData.endDate) : null),
      requestId: new FormControl<string>(dialogData.requestId ?? ''),
      transactionId: new FormControl<string>(dialogData.transactionId ?? ''),
      httpStatuses: new FormControl<string[]>(dialogData.httpStatuses?.map(hs => hs.value) ?? []),
      messageText: new FormControl<string>(dialogData.messageText ?? ''),
      path: new FormControl<string>(dialogData.path ?? ''),
    });
  }

  onCancel() {
    this.dialogRef.close(this.initialData);
  }

  onApply() {
    const formValue = this.moreFiltersForm.getRawValue();
    const httpStatuses = this.httpStatusChoices.filter(x => formValue.httpStatuses?.includes(x.value));

    this.dialogRef.close({
      startDate: formValue.startDate?.getTime(),
      endDate: formValue.endDate?.getTime(),
      ...(formValue.requestId?.length ? { requestId: formValue.requestId } : {}),
      ...(formValue.transactionId?.length ? { transactionId: formValue.transactionId } : {}),
      ...(formValue.httpStatuses?.length ? { httpStatuses } : {}),
      ...(formValue.messageText?.length ? { messageText: formValue.messageText } : {}),
      ...(formValue.path?.length ? { path: formValue.path } : {}),
    });
  }

  startDateFilter = (d: Date | null): boolean => {
    return !d || d.getTime() < Date.now();
  };

  endDateFilter = (d: Date | null): boolean => {
    if (!d) {
      return true;
    }

    const startDayTime = this.moreFiltersForm.getRawValue().startDate?.getTime();
    const currentTime = Date.now();

    return (!startDayTime || startDayTime < d.getTime()) && d.getTime() < currentTime;
  };
}
