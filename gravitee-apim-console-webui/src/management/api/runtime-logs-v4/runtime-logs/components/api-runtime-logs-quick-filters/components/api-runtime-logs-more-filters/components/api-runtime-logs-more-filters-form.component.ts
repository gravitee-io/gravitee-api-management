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
import { FormControl, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { Moment } from 'moment';

import { DEFAULT_PERIOD, MoreFiltersForm, PERIODS } from '../../../../../models';

@Component({
  selector: 'api-runtime-logs-more-filters-form',
  template: require('./api-runtime-logs-more-filters-form.component.html'),
  styles: [require('./api-runtime-logs-more-filters-form.component.scss')],
})
export class ApiRuntimeLogsMoreFiltersFormComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() formValues: MoreFiltersForm;
  @Output() valuesChangeEvent: EventEmitter<MoreFiltersForm> = new EventEmitter<MoreFiltersForm>();
  @Output() isInvalidEvent: EventEmitter<boolean> = new EventEmitter<boolean>();
  readonly periods = PERIODS;
  datesForm: FormGroup;
  minDate: Moment;

  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.datesForm = new FormGroup({
      period: new FormControl(this.formValues.period),
      from: new FormControl(this.formValues.from),
      to: new FormControl(this.formValues.to),
    });
    this.minDate = this.formValues.from;
    this.onDatesChange();
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

  private emitValues() {
    this.datesForm.updateValueAndValidity({ emitEvent: false });
    this.isInvalidEvent.emit(this.datesForm.invalid);
    this.valuesChangeEvent.emit(this.datesForm.getRawValue());
    this.cdr.detectChanges();
  }
}
