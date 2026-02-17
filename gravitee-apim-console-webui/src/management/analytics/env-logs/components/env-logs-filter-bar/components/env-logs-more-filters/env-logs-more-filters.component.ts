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
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, input, output } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Moment } from 'moment';
import { map } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { DEFAULT_MORE_FILTERS, EnvLogsMoreFiltersForm } from '../../../../models/env-logs-more-filters.model';
import { DATE_TIME_FORMATS } from '../../../../../../../shared/utils/timeFrameRanges';
import { HTTP_METHODS } from '../../../../../../../entities/management-api-v2';

// TODO: Replace with data from API when backend integration is implemented
export const MOCK_ENTRYPOINTS = [
  { id: 'http-proxy', name: 'HTTP Proxy' },
  { id: 'http-get', name: 'HTTP GET' },
  { id: 'http-post', name: 'HTTP POST' },
  { id: 'sse', name: 'SSE' },
  { id: 'websocket', name: 'WebSocket' },
  { id: 'webhook', name: 'Webhook' },
];

// TODO: Replace with data from API when backend integration is implemented
export const MOCK_PLANS = [
  { id: 'plan-1', name: 'Free Plan' },
  { id: 'plan-2', name: 'Gold Plan' },
  { id: 'plan-3', name: 'Enterprise Plan' },
];

type MoreFiltersFormGroup = {
  from: FormControl<Moment | null>;
  to: FormControl<Moment | null>;
  statuses: FormControl<Set<number>>;
  entrypoints: FormControl<string[] | null>;
  methods: FormControl<string[] | null>;
  plans: FormControl<string[] | null>;
  mcpMethod: FormControl<string | null>;
  transactionId: FormControl<string | null>;
  requestId: FormControl<string | null>;
  uri: FormControl<string | null>;
  responseTime: FormControl<number | null>;
};

@Component({
  selector: 'env-logs-more-filters',
  templateUrl: './env-logs-more-filters.component.html',
  styleUrl: './env-logs-more-filters.component.scss',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    MatButtonModule,
    MatChipsModule,
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
export class EnvLogsMoreFiltersComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  showMoreFilters = input(false);
  formValues = input<EnvLogsMoreFiltersForm>(DEFAULT_MORE_FILTERS);

  closeMoreFilters = output<void>();
  applyMoreFilters = output<EnvLogsMoreFiltersForm>();

  readonly httpMethods = HTTP_METHODS;
  readonly entrypoints = MOCK_ENTRYPOINTS;
  readonly plans = MOCK_PLANS;

  form = this.fb.group<MoreFiltersFormGroup>({
    from: this.fb.control<Moment | null>(null),
    to: this.fb.control<Moment | null>(null),
    statuses: this.fb.control<Set<number>>(new Set(), { nonNullable: true }),
    entrypoints: this.fb.control<string[] | null>(null),
    methods: this.fb.control<string[] | null>(null),
    plans: this.fb.control<string[] | null>(null),
    // TODO: Add Validators (e.g. pattern, minLength, min) to these controls when backend integration is implemented
    mcpMethod: this.fb.control<string | null>(null),
    transactionId: this.fb.control<string | null>(null),
    requestId: this.fb.control<string | null>(null),
    uri: this.fb.control<string | null>(null),
    responseTime: this.fb.control<number | null>(null),
  });
  isInvalid = toSignal(this.form.statusChanges.pipe(map(() => this.form.invalid)), { initialValue: false });
  minDate: Moment | null = null;
  statuses: Set<number> = new Set();

  get minDateDisplay(): Date | null {
    return this.minDate ? this.minDate.toDate() : null;
  }

  constructor() {
    this.initSideEffects();
  }

  addStatusFromInput(event: MatChipInputEvent): void {
    const value = event.value?.trim();
    if (value && !isNaN(+value)) {
      this.statuses.add(+value);
      this.form.controls.statuses.setValue(new Set(this.statuses));
      event.chipInput?.clear();
    }
  }

  removeStatus(status: number): void {
    this.statuses.delete(status);
    this.form.controls.statuses.setValue(new Set(this.statuses));
  }

  resetMoreFilters(): void {
    this.form.patchValue({
      from: null,
      to: null,
      entrypoints: null,
      methods: null,
      plans: null,
      mcpMethod: null,
      transactionId: null,
      requestId: null,
      uri: null,
      responseTime: null,
    });
    this.statuses = new Set();
    this.form.controls.statuses.setValue(new Set());
    this.minDate = null;
    this.apply();
  }

  close(): void {
    this.closeMoreFilters.emit();
  }

  apply(): void {
    const raw = this.form.getRawValue();
    this.applyMoreFilters.emit({
      from: raw.from ?? null,
      to: raw.to ?? null,
      statuses: this.statuses.size > 0 ? new Set(this.statuses) : new Set(),
      entrypoints: raw.entrypoints ?? null,
      methods: raw.methods ?? null,
      plans: raw.plans ?? null,
      // TODO: Add input sanitization/validation when backend integration is implemented
      mcpMethod: raw.mcpMethod?.trim() || null,
      transactionId: raw.transactionId?.trim() || null,
      requestId: raw.requestId?.trim() || null,
      uri: raw.uri?.trim() || null,
      responseTime: raw.responseTime ?? null,
    });
    this.close();
  }

  private updateFormFromInput(formValues: EnvLogsMoreFiltersForm): void {
    if (!this.form) return;

    this.form.patchValue(
      {
        from: formValues?.from ?? null,
        to: formValues?.to ?? null,
        entrypoints: formValues?.entrypoints ?? null,
        methods: formValues?.methods ?? null,
        plans: formValues?.plans ?? null,
        mcpMethod: formValues?.mcpMethod ?? null,
        transactionId: formValues?.transactionId ?? null,
        requestId: formValues?.requestId ?? null,
        uri: formValues?.uri ?? null,
        responseTime: formValues?.responseTime ?? null,
      },
      { emitEvent: false },
    );

    this.statuses = new Set(formValues?.statuses ?? []);
    this.form.controls.statuses.setValue(new Set(this.statuses), { emitEvent: false });
    this.minDate = formValues?.from ?? null;
  }

  private initSideEffects(): void {
    effect(() => {
      this.updateFormFromInput(this.formValues());
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(from => {
      this.minDate = from ?? null;
    });
  }
}
