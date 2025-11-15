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
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import moment, { Moment } from 'moment';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';

import { DEFAULT_PERIOD, PERIODS } from '../../../../../runtime-logs/models';
import { DATE_TIME_FORMATS } from '../../../../../../../../shared/utils/timeFrameRanges';
import { WebhookMoreFiltersForm } from '../../../../models';

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
export class WebhookLogsMoreFiltersFormComponent implements OnInit, OnChanges, OnDestroy {
  private readonly unsubscribe$ = new Subject<void>();

  @Input() formValues: WebhookMoreFiltersForm = { period: DEFAULT_PERIOD, from: null, to: null };
  @Input() callbackUrls: string[] = [];
  @Output() valuesChangeEvent = new EventEmitter<WebhookMoreFiltersForm>();
  @Output() isInvalidEvent = new EventEmitter<boolean>();

  readonly periods = PERIODS;
  datesForm = new UntypedFormGroup({
    period: new UntypedFormControl(DEFAULT_PERIOD),
    from: new UntypedFormControl(null),
    to: new UntypedFormControl(null),
  });
  moreFiltersForm = new UntypedFormGroup({
    callbackUrls: new UntypedFormControl([]),
  });
  minDate: Moment | null = null;

  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.patchForm(this.formValues);
    this.moreFiltersForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => this.emitValues());
    this.registerListeners();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.formValues && !changes.formValues.firstChange) {
      this.patchForm(changes.formValues.currentValue as WebhookMoreFiltersForm);
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  private registerListeners(): void {
    this.datesForm
      .get('period')
      ?.valueChanges.pipe(
        tap(() => {
          this.datesForm.get('from')?.setValue(null, { emitEvent: false, onlySelf: true });
          this.datesForm.get('to')?.setValue(null, { emitEvent: false, onlySelf: true });
          this.minDate = null;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());

    this.datesForm
      .get('from')
      ?.valueChanges.pipe(
        tap((from) => {
          this.minDate = from ?? null;
          this.datesForm.get('period')?.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());

    this.datesForm
      .get('to')
      ?.valueChanges.pipe(
        tap(() => this.datesForm.get('period')?.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true })),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.emitValues());
  }

  private patchForm(values: WebhookMoreFiltersForm = { period: DEFAULT_PERIOD, from: null, to: null }): void {
    const period = values?.period ?? DEFAULT_PERIOD;
    const from = values?.from ? moment(values.from) : null;
    const to = values?.to ? moment(values.to) : null;

    this.datesForm.setValue(
      {
        period,
        from,
        to,
      },
      { emitEvent: false },
    );
    this.moreFiltersForm.setValue({ callbackUrls: values?.callbackUrls ?? [] }, { emitEvent: false });
    this.minDate = from;
    this.emitValues();
  }

  private emitValues(): void {
    this.datesForm.updateValueAndValidity({ emitEvent: false });
    this.isInvalidEvent.emit(this.datesForm.invalid);
    const raw = this.datesForm.getRawValue();
    this.valuesChangeEvent.emit({
      period: raw.period ?? DEFAULT_PERIOD,
      from: raw.from ?? null,
      to: raw.to ?? null,
      callbackUrls: this.moreFiltersForm.get('callbackUrls')?.value ?? [],
    });
    this.cdr.detectChanges();
  }
}
