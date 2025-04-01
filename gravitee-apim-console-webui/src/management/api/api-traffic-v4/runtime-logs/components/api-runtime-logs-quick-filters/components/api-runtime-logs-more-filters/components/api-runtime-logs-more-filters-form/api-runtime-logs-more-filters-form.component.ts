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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { Moment } from 'moment';
import { MatChipInputEvent } from '@angular/material/chips';

import { DEFAULT_FILTERS, DEFAULT_PERIOD, MoreFiltersForm, MultiFilter, PERIODS } from '../../../../../../models';

@Component({
  selector: 'api-runtime-logs-more-filters-form',
  templateUrl: './api-runtime-logs-more-filters-form.component.html',
  styleUrls: ['./api-runtime-logs-more-filters-form.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsMoreFiltersFormComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() formValues: MoreFiltersForm;
  @Output() valuesChangeEvent: EventEmitter<MoreFiltersForm> = new EventEmitter<MoreFiltersForm>();
  @Output() isInvalidEvent: EventEmitter<boolean> = new EventEmitter<boolean>();
  readonly periods = PERIODS;
  datesForm: UntypedFormGroup;
  moreFiltersForm: UntypedFormGroup;
  minDate: Moment;
  statuses: Set<number>;
  applicationsCache: MultiFilter;

  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.datesForm = new UntypedFormGroup({
      period: new UntypedFormControl(this.formValues.period),
      from: new UntypedFormControl(this.formValues.from),
      to: new UntypedFormControl(this.formValues.to),
    });
    this.minDate = this.formValues.from;
    this.onDatesChange();

    this.statuses = new Set(this.formValues.statuses);
    this.applicationsCache = this.formValues.applications;
    this.moreFiltersForm = new UntypedFormGroup({
      statuses: new UntypedFormControl(this.statuses),
      applications: new UntypedFormControl(this.formValues.applications?.map((application) => application.value)),
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

  addStatusFromInput(event: MatChipInputEvent) {
    if (event.value && !isNaN(+event.value)) {
      this.statuses.add(+event.value);
      this.moreFiltersForm.get('statuses').setValue(this.statuses);
      event.chipInput?.clear();
    }
  }

  removeStatus(status: number) {
    this.statuses.delete(status);
    this.moreFiltersForm.get('statuses').setValue(this.statuses);
  }

  private emitValues() {
    this.datesForm.updateValueAndValidity({ emitEvent: false });
    this.isInvalidEvent.emit(this.datesForm.invalid);
    this.valuesChangeEvent.emit({
      ...this.datesForm.getRawValue(),
      statuses: this.statuses.size > 0 ? this.statuses : null,
      applications: this.applicationsFromValues(this.moreFiltersForm.get('applications').value),
    });
    this.cdr.detectChanges();
  }

  private applicationsFromValues(ids: string[]): MultiFilter {
    return ids?.length > 0 ? ids.map((id) => this.applicationsCache.find((app) => app.value === id)) : DEFAULT_FILTERS.applications;
  }
}
