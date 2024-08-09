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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_FORMATS, MAT_NATIVE_DATE_FORMATS, MatOption, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';

import { ApplicationTabLogsService, HttpStatusVM } from '../../application-tab-logs.service';

export interface MoreFiltersDialogData {
  startDate?: number;
  endDate?: number;
  requestId?: string;
  transactionId?: string;
  httpStatuses?: HttpStatusVM[];
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
  standalone: true,
  styleUrl: './more-filters-dialog.component.scss',
  templateUrl: './more-filters-dialog.component.html',
})
export class MoreFiltersDialogComponent {
  startDate: Date | undefined;
  endDate: Date | undefined;
  requestId: string | undefined;
  transactionId: string | undefined;
  httpStatuses: string[] | undefined;

  httpStatusChoices = inject(ApplicationTabLogsService).httpStatuses;

  private readonly initialData: MoreFiltersDialogData;

  constructor(
    private readonly dialogRef: MatDialogRef<MoreFiltersDialogComponent, MoreFiltersDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: MoreFiltersDialogData,
  ) {
    this.initialData = dialogData;

    this.startDate = dialogData.startDate ? new Date(dialogData.startDate) : undefined;
    this.endDate = dialogData.endDate ? new Date(dialogData.endDate) : undefined;
    this.requestId = dialogData.requestId ?? '';
    this.transactionId = dialogData.transactionId ?? '';
    this.httpStatuses = dialogData.httpStatuses?.map(hs => hs.value) ?? [];
  }

  onCancel() {
    this.dialogRef.close(this.initialData);
  }

  onApply() {
    const httpStatuses = this.httpStatusChoices.filter(x => this.httpStatuses?.includes(x.value));

    this.dialogRef.close({
      startDate: this.startDate?.getTime(),
      endDate: this.endDate?.getTime(),
      requestId: this.requestId,
      transactionId: this.transactionId,
      ...(this.httpStatuses ? { httpStatuses } : {}),
    });
  }

  startDateFilter = (d: Date | null): boolean => {
    return !d || d.getTime() < Date.now();
  };

  endDateFilter = (d: Date | null): boolean => {
    if (!d) {
      return true;
    }

    const startDayTime = this.startDate?.getTime();
    const currentTime = Date.now();

    return (!startDayTime || startDayTime < d.getTime()) && d.getTime() < currentTime;
  };
}
