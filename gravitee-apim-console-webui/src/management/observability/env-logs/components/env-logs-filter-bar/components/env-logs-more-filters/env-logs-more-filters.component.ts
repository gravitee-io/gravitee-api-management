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
import { merge, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';
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
import { AnalyticsService } from '../../../../../../../services-ngx/analytics.service';
import { SnackBarService } from '../../../../../../../services-ngx/snack-bar.service';
import { ApiPlanV2Service } from '../../../../../../../services-ngx/api-plan-v2.service';

export const ENTRYPOINT_TYPES = [
  { id: 'http-proxy', name: 'HTTP Proxy' },
  { id: 'http-get', name: 'HTTP GET' },
  { id: 'http-post', name: 'HTTP POST' },
  { id: 'sse', name: 'SSE' },
  { id: 'websocket', name: 'WebSocket' },
  { id: 'webhook', name: 'Webhook' },
];

/** Matches Gravitee entity IDs (UUID v4). Case-insensitive to accept both user-typed and copy-pasted values. */
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Valid URI path characters. Uses `*` (zero-or-more) because the field is optional; empty values are trimmed to null on apply(). */
const URI_PATTERN = /^[\w\-./~:@!$&'()*+,;=%?#[\]]*$/;

type MoreFiltersFormGroup = {
  from: FormControl<Moment | null>;
  to: FormControl<Moment | null>;
  statuses: FormControl<Set<number>>;
  entrypoints: FormControl<string[] | null>;
  methods: FormControl<string[] | null>;
  plans: FormControl<string[] | null>;
  transactionId: FormControl<string | null>;
  requestId: FormControl<string | null>;
  uri: FormControl<string | null>;
  responseTime: FormControl<number | null>;
  errorKeys: FormControl<string[] | null>;
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
  private readonly analyticsService = inject(AnalyticsService);
  private readonly snackBarService = inject(SnackBarService);
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

  minDate: Moment | null = null;
  statuses: Set<number> = new Set();
  errorKeysOptions: string[] = [];

  /**
   * Whether to show the Plans dropdown.
   * Plans are API-scoped (no env-level endpoint), so we can only show them
   * when exactly ONE API is selected.
   */
  showPlans = computed(() => this.selectedApiIds().length === 1);

  /**
   * Dynamically loaded plans for the selected API.
   * Caches results per API to avoid repeated fetches when toggling selection.
   */
  private readonly plansCache = new Map<string, { id: string; name: string }[]>();
  private readonly plans$ = toObservable(this.selectedApiIds).pipe(switchMap(apiIds => this.loadPlansForApis(apiIds)));

  readonly plans = toSignal(this.plans$, { initialValue: [] as { id: string; name: string }[] });

  form = this.fb.group<MoreFiltersFormGroup>({
    from: this.fb.control<Moment | null>(null),
    to: this.fb.control<Moment | null>(null),
    statuses: this.fb.control<Set<number>>(new Set(), { nonNullable: true }),
    entrypoints: this.fb.control<string[] | null>(null),
    methods: this.fb.control<string[] | null>(null),
    plans: this.fb.control<string[] | null>(null),
    transactionId: this.fb.control<string | null>(null, [Validators.maxLength(36), Validators.pattern(UUID_PATTERN)]),
    requestId: this.fb.control<string | null>(null, [Validators.maxLength(36), Validators.pattern(UUID_PATTERN)]),
    uri: this.fb.control<string | null>(null, [Validators.maxLength(2048), Validators.pattern(URI_PATTERN)]),
    responseTime: this.fb.control<number | null>(null, [Validators.min(0)]),
    errorKeys: this.fb.control<string[] | null>(null),
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
      transactionId: null,
      requestId: null,
      uri: null,
      responseTime: null,
      errorKeys: null,
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
      transactionId: raw.transactionId?.trim() || null,
      requestId: raw.requestId?.trim() || null,
      uri: raw.uri?.trim() || null,
      responseTime: raw.responseTime ?? null,
      errorKeys: raw.errorKeys ?? null,
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
        transactionId: formValues?.transactionId ?? null,
        requestId: formValues?.requestId ?? null,
        uri: formValues?.uri ?? null,
        responseTime: formValues?.responseTime ?? null,
        errorKeys: formValues?.errorKeys ?? null,
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
      const from = this.formValues().from?.valueOf() ?? Date.now() - 24 * 60 * 60 * 1000;
      const to = this.formValues().to?.valueOf() ?? Date.now();
      this.analyticsService
        .getEnvironmentErrorKeys(from, to)
        .pipe(
          catchError(() => of([])),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(keys => this.updateErrorKeysOptions(keys));
    });

    // When the Plans dropdown is hidden (0 or 2+ APIs selected), clear any plan selections
    effect(() => {
      if (!this.showPlans()) {
        this.form.controls.plans.setValue(null, { emitEvent: false });
      }
    });

    merge(this.form.controls.from.valueChanges, this.form.controls.to.valueChanges)
      .pipe(
        tap(() => {
          this.minDate = this.form.controls.from.value ?? null;
        }),
        debounceTime(200),
        switchMap(() => {
          const from = this.form.controls.from.value?.valueOf();
          const to = this.form.controls.to.value?.valueOf();
          if (!from || !to) return of(this.errorKeysOptions);
          return this.analyticsService.getEnvironmentErrorKeys(from, to).pipe(catchError(() => of([])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(keys => this.updateErrorKeysOptions(keys));
  }

  private updateErrorKeysOptions(keys: string[]) {
    this.errorKeysOptions = keys;
    if (keys.length) {
      this.form.controls.errorKeys.enable({ emitEvent: false });
    } else {
      this.form.controls.errorKeys.disable({ emitEvent: false });
    }
  }

  private loadPlansForApis(apiIds: string[]) {
    if (apiIds.length !== 1) return of([]);
    const apiId = apiIds[0];
    const cached = this.plansCache.get(apiId);
    if (cached) return of(cached);
    return this.planService.list(apiId, undefined, undefined, undefined, undefined, 1, 9999).pipe(
      map(response => {
        const plans = response.data.map(plan => ({ id: plan.id, name: plan.name }));
        this.plansCache.set(apiId, plans);
        return plans;
      }),
      catchError(() => of([])),
    );
  }
}
