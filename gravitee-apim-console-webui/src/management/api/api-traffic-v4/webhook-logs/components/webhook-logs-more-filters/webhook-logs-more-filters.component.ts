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
import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, EventEmitter, input, Input, OnInit, Output, effect } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Moment } from 'moment';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { DEFAULT_PERIOD, PERIODS, SimpleFilter } from '../../../runtime-logs/models';
import { DATE_TIME_FORMATS } from '../../../../../../shared/utils/timeFrameRanges';
import { WebhookMoreFiltersForm } from '../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-more-filters',
  templateUrl: './webhook-logs-more-filters.component.html',
  styleUrls: ['./webhook-logs-more-filters.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    GioIconsModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
})
export class WebhookLogsMoreFiltersComponent implements OnInit {
  @Output() closeMoreFiltersEvent = new EventEmitter<void>();
  @Output() applyMoreFiltersEvent = new EventEmitter<WebhookMoreFiltersForm>();
  showMoreFilters = input(false);
  formValues = input<WebhookMoreFiltersForm>({ period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
  @Input() callbackUrls: string[] = [];

  form: FormGroup<{
    period: FormControl<SimpleFilter | null>;
    from: FormControl<Moment | null>;
    to: FormControl<Moment | null>;
    callbackUrls: FormControl<string[]>;
  }>;
  isInvalid = false;
  minDate: Moment | null = null;
  minDateDisplay: Date | null = null;
  readonly periods = PERIODS;

  constructor(
    private readonly fb: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    this.form = this.fb.group({
      period: this.fb.control<SimpleFilter | null>(DEFAULT_PERIOD),
      from: this.fb.control<Moment | null>(null),
      to: this.fb.control<Moment | null>(null),
      callbackUrls: this.fb.control<string[]>([], { nonNullable: true }),
    });
    effect(() => {
      this.updateFormFromInput(this.formValues());
    });
  }

  ngOnInit(): void {
    this.isInvalid = this.form.invalid;

    this.form.statusChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.isInvalid = this.form.invalid;
    });

    this.form.controls.period.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.controls.from.setValue(null, { emitEvent: false, onlySelf: true });
      this.form.controls.to.setValue(null, { emitEvent: false, onlySelf: true });
      this.minDate = null;
      this.minDateDisplay = null;
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(from => {
      this.minDate = from ?? null;
      this.minDateDisplay = from ? from.toDate() : null;
      this.form.controls.period.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
    });

    this.form.controls.to.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.controls.period.setValue(DEFAULT_PERIOD, { emitEvent: false, onlySelf: true });
    });
  }

  private updateFormFromInput(formValues: WebhookMoreFiltersForm): void {
    if (this.form) {
      const values: WebhookMoreFiltersForm = {
        period: formValues?.period ?? DEFAULT_PERIOD,
        from: formValues?.from ?? null,
        to: formValues?.to ?? null,
        callbackUrls: formValues?.callbackUrls ?? [],
      };
      this.form.patchValue(
        {
          period: values.period ?? DEFAULT_PERIOD,
          from: values.from ?? null,
          to: values.to ?? null,
          callbackUrls: values.callbackUrls ?? [],
        },
        { emitEvent: false },
      );
      this.minDate = values.from ?? null;
      this.minDateDisplay = values.from ? values.from.toDate() : null;
      this.isInvalid = this.form.invalid;
    }
  }

  resetMoreFilters(): void {
    this.form.patchValue({ period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] });
    this.minDate = null;
    this.minDateDisplay = null;
    this.apply();
  }

  close(): void {
    this.closeMoreFiltersEvent.emit();
  }

  apply(): void {
    const rawValue = this.form.getRawValue();
    const value: WebhookMoreFiltersForm = {
      period: rawValue.period ?? undefined,
      from: rawValue.from ?? null,
      to: rawValue.to ?? null,
      callbackUrls: rawValue.callbackUrls ?? [],
    };
    this.applyMoreFiltersEvent.emit(value);
    this.close();
  }
}
