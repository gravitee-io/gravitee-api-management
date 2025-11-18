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
import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { Moment } from 'moment';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';

import { DEFAULT_PERIOD, PERIODS } from '../../../../../runtime-logs/models';
import { DATE_TIME_FORMATS } from '../../../../../../../../shared/utils/timeFrameRanges';
import { WebhookMoreFiltersForm } from '../../../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-more-filters-form',
  templateUrl: './webhook-logs-more-filters-form.component.html',
  styleUrls: ['./webhook-logs-more-filters-form.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatIconModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
})
export class WebhookLogsMoreFiltersFormComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() formValues: WebhookMoreFiltersForm;
  @Input() callbackUrls: string[] = [];
  @Output() valuesChangeEvent: EventEmitter<WebhookMoreFiltersForm> = new EventEmitter<WebhookMoreFiltersForm>();
  @Output() isInvalidEvent: EventEmitter<boolean> = new EventEmitter<boolean>();

  readonly periods = PERIODS;
  datesForm: UntypedFormGroup;
  moreFiltersForm: UntypedFormGroup;
  minDate: Moment;

  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.datesForm = new UntypedFormGroup({
      period: new UntypedFormControl(this.formValues.period ?? DEFAULT_PERIOD),
      from: new UntypedFormControl(this.formValues.from),
      to: new UntypedFormControl(this.formValues.to),
    });
    this.minDate = this.formValues.from;
    this.onDatesChange();

    this.moreFiltersForm = new UntypedFormGroup({
      callbackUrls: new UntypedFormControl(this.formValues.callbackUrls ?? []),
    });
    this.moreFiltersForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.emitValues());
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onDatesChange() {
    this.datesForm
      .get('period')
      .valueChanges.pipe(
        tap(() => {
          this.datesForm.get('from').setValue(null, { emitEvent: false, onlySelf: true });
          this.datesForm.get('to').setValue(null, { emitEvent: false, onlySelf: true });
          this.minDate = null;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());

    this.datesForm
      .get('from')
      .valueChanges.pipe(
        tap((from) => {
          this.minDate = from;
          this.datesForm.get('period').setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());

    this.datesForm
      .get('to')
      .valueChanges.pipe(
        tap(() => this.datesForm.get('period').setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true })),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());
  }

  private emitValues() {
    this.datesForm.updateValueAndValidity({ emitEvent: false });
    this.isInvalidEvent.emit(this.datesForm.invalid);
    this.valuesChangeEvent.emit({
      ...this.datesForm.getRawValue(),
      callbackUrls: this.moreFiltersForm.get('callbackUrls').value ?? [],
    });
    this.cdr.detectChanges();
  }
}
