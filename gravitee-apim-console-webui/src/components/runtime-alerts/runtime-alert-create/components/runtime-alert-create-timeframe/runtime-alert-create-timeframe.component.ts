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
import { Component, DestroyRef, forwardRef } from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor, FormArray, FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import moment from "moment";
import { tap } from "rxjs/operators";
import { isEqual } from "lodash";

import { Days } from "../../../../../entities/alerts/notificationPeriod";

export type TimeframeFormValue = {
  days: string[];
  timeRange: moment.Moment[];
  businessDays: boolean;
  officeHours: boolean;
};

export interface TimeframeControlInterface {
  days: FormControl<string[]>,
  timeRange: FormControl<moment.Moment[]>,
  businessDays: FormControl<boolean>,
  officeHours: FormControl<boolean>
}

@Component({
  selector: "runtime-alert-create-timeframe",
  templateUrl: "./runtime-alert-create-timeframe.component.html",
  styleUrls: ["./runtime-alert-create-timeframe.component.scss"],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RuntimeAlertCreateTimeframeComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RuntimeAlertCreateTimeframeComponent),
      multi: true
    }
  ]
})
export class RuntimeAlertCreateTimeframeComponent implements ControlValueAccessor, Validator {
  private officeHours = [moment("09:00", "HH:mm"), moment("18:00", "HH:mm")];
  private _onChange: (value: TimeframeFormValue) => void = () => ({});
  private _onTouched: () => void = () => ({});

  protected form: FormGroup;
  protected days = Days.getAllDayNames();
  protected businessDay = Days.getBusinessDays();

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    this.form = this.formBuilder.group({
      timeframes: this.formBuilder.array([])
    });

    this.form.valueChanges
      .pipe(
        tap((value) => {
          this._onChange(value);
          this._onTouched();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  get timeframesControl(): FormArray {
    return this.form.get("timeframes") as FormArray;
  }

  public addTimeframe() {
    const timeframeControl: FormGroup<TimeframeControlInterface> = this.formBuilder.group({
      days: new FormControl<string[]>([]),
      timeRange: new FormControl<moment.Moment[]>(null),
      businessDays: new FormControl<boolean>(false),
      officeHours: new FormControl<boolean>(false)
    });
    this.syncDaysFormFields(timeframeControl);
    this.syncTimeRangeFormFields(timeframeControl)
    this.timeframesControl.push(timeframeControl);
  }

  public deleteTimeframe(index: number) {
    this.timeframesControl.removeAt(index);
  }

  public getGroup(control: AbstractControl) {
    return control as FormGroup;
  }

  writeValue(value: TimeframeFormValue): void {
    if (value) {
      this.form.setValue(value, { emitEvent: false });
    }
  }

  registerOnChange(fn: (value: TimeframeFormValue) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.form.disable() : this.form.enable();
  }

  validate(_: AbstractControl): ValidationErrors | null {
    return this.form.valid ? null : { invalidForm: { valid: false, message: "Timeframe form is invalid" } };
  }

  private syncDaysFormFields(formGroup: FormGroup<TimeframeControlInterface>) {
    formGroup.controls.businessDays.valueChanges
      .pipe(
        tap((value) => formGroup.controls.days.patchValue(value ? this.businessDay : null, { emitEvent: false })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();

    formGroup.controls.days.valueChanges
      .pipe(
        tap((value) => formGroup.controls.businessDays.patchValue(isEqual(value, this.businessDay), { emitEvent: false })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private syncTimeRangeFormFields(formGroup: FormGroup<TimeframeControlInterface>) {
    formGroup.controls.timeRange.valueChanges
      .pipe(
        tap((value: moment.Moment[]) =>
          formGroup.controls.officeHours.patchValue(this.isOfficeHours(value), {
            emitEvent: false,
          }),
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();

    formGroup.controls.officeHours.valueChanges
      .pipe(
        tap((value) =>
          formGroup.controls.timeRange.patchValue(value ? this.officeHours : null, {
            emitEvent: false,
          }),
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private isOfficeHours(value: moment.Moment[]) {
    return (
      value?.length === 2 &&
      value[0] != null &&
      value[1] != null &&
      this.toTime(value[0]) === this.toTime(this.officeHours[0]) &&
      this.toTime(value[1]) === this.toTime(this.officeHours[1])
    );
  }

  private toTime(m: moment.Moment) {
    return m.format("HH:mm:ss");
  }
}
