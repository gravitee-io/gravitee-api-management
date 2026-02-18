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
import { Component, DestroyRef, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Moment } from 'moment';
import { map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
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
import { ApiPlanV2Service } from '../../../../../../../services-ngx/api-plan-v2.service';

/**
 * Known V4 entrypoint connector types.
 * These are a fixed set defined by the Gravitee platform — not API-specific.
 */
export const ENTRYPOINT_TYPES = [
  { id: 'http-proxy', name: 'HTTP Proxy' },
  { id: 'http-get', name: 'HTTP GET' },
  { id: 'http-post', name: 'HTTP POST' },
  { id: 'sse', name: 'SSE' },
  { id: 'websocket', name: 'WebSocket' },
  { id: 'webhook', name: 'Webhook' },
];

/** UUID v4 pattern (lowercase hex with dashes). */
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

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
  private readonly planService = inject(ApiPlanV2Service);

  showMoreFilters = input(false);
  formValues = input<EnvLogsMoreFiltersForm>(DEFAULT_MORE_FILTERS);

  /**
   * IDs of the APIs currently selected in the quick filter bar.
   * When exactly one API is selected, Plans are fetched dynamically.
   * When 0 or 2+ APIs are selected, the Plans dropdown is hidden.
   */
  selectedApiIds = input<string[]>([]);

  closeMoreFilters = output<void>();
  applyMoreFilters = output<EnvLogsMoreFiltersForm>();

  readonly httpMethods = HTTP_METHODS;
  readonly entrypoints = ENTRYPOINT_TYPES;

  // 4. State
  minDate: Moment | null = null;
  statuses: Set<number> = new Set();

  /**
   * Whether to show the Plans dropdown.
   * Plans are API-scoped (no env-level endpoint), so we can only show them
   * when exactly ONE API is selected.
   */
  showPlans = computed(() => this.selectedApiIds().length === 1);

  /**
   * Dynamically loaded plans for the selected API.
   * Uses `toObservable` + `switchMap` to reactively fetch plans whenever `selectedApiIds` changes.
   */
  private readonly plans$ = toObservable(this.selectedApiIds).pipe(
    switchMap(apiIds => {
      if (apiIds.length !== 1) return of([]);
      return this.planService
        .list(apiIds[0], undefined, undefined, undefined, undefined, 1, 100)
        .pipe(map(response => response.data.map(plan => ({ id: plan.id, name: plan.name }))));
    }),
  );

  readonly plans = toSignal(this.plans$, { initialValue: [] as { id: string; name: string }[] });

  form = this.fb.group<MoreFiltersFormGroup>({
    from: this.fb.control<Moment | null>(null),
    to: this.fb.control<Moment | null>(null),
    statuses: this.fb.control<Set<number>>(new Set(), { nonNullable: true }),
    entrypoints: this.fb.control<string[] | null>(null),
    methods: this.fb.control<string[] | null>(null),
    plans: this.fb.control<string[] | null>(null),
    mcpMethod: this.fb.control<string | null>(null),
    transactionId: this.fb.control<string | null>(null, [Validators.maxLength(36), Validators.pattern(UUID_PATTERN)]),
    requestId: this.fb.control<string | null>(null, [Validators.maxLength(36), Validators.pattern(UUID_PATTERN)]),
    uri: this.fb.control<string | null>(null, [Validators.maxLength(2048)]),
    responseTime: this.fb.control<number | null>(null, [Validators.min(0)]),
  });
  isInvalid = toSignal(this.form.statusChanges.pipe(map(() => this.form.invalid)), { initialValue: false });

  get minDateDisplay(): Date | null {
    return this.minDate ? this.minDate.toDate() : null;
  }

  constructor() {
    this.initSideEffects();
  }

  addStatusFromInput(event: MatChipInputEvent): void {
    const value = event.value?.trim();
    if (value && !isNaN(+value)) {
      const code = +value;
      if (code >= 100 && code <= 599 && Number.isInteger(code)) {
        this.statuses.add(code);
        this.form.controls.statuses.setValue(new Set(this.statuses));
      }
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

    // When the Plans dropdown is hidden (0 or 2+ APIs selected), clear any plan selections
    effect(() => {
      if (!this.showPlans()) {
        this.form.controls.plans.setValue(null, { emitEvent: false });
      }
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(from => {
      this.minDate = from ?? null;
    });
  }
}
