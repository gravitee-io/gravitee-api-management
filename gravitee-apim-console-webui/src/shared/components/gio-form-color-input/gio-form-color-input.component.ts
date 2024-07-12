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
import { FocusMonitor } from '@angular/cdk/a11y';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Component, DoCheck, ElementRef, HostBinding, Input, OnDestroy, Optional, Self } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormControl,
  NgControl,
  NG_VALIDATORS,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { MatFormFieldControl } from '@angular/material/form-field';
import { isEmpty } from 'lodash';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';

export type Color = string;

export const colorValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  if (isEmpty(control.value)) {
    return null;
  }

  const badColor = /^#[0-9a-f]{6}$/i.test(control.value);
  return !badColor ? { color: { message: `"${control.value}" is not a valid color` } } : null;
};

@Component({
  selector: 'gio-form-color-input',
  templateUrl: './gio-form-color-input.component.html',
  styleUrls: ['./gio-form-color-input.component.scss'],
  host: {
    '[id]': 'id',
    '[attr.aria-describedby]': 'describedBy',
  },
  providers: [
    {
      provide: MatFormFieldControl,
      useExisting: GioFormColorInputComponent,
    },
    {
      provide: NG_VALIDATORS,
      useValue: colorValidator,
      multi: true,
    },
  ],
})
export class GioFormColorInputComponent implements MatFormFieldControl<Color>, ControlValueAccessor, DoCheck, OnDestroy {
  static nextId = 0;

  colorFormControl = new UntypedFormControl();

  _onChange: (value: any) => void = () => ({});

  _onTouched: () => any = () => ({});
  touched = false;

  // From ControlValueAccessor interface
  get value(): Color | null {
    return this._value;
  }

  set value(_color: Color | null) {
    this._value = _color;
    this._onChange(_color);
    this.stateChanges.next();
  }

  private _value: Color | null = null;

  // From ControlValueAccessor interface
  stateChanges = new Subject<void>();

  // From ControlValueAccessor interface
  @HostBinding()
  id = `gio-form-color-input-${GioFormColorInputComponent.nextId++}`;

  // From ControlValueAccessor interface
  @Input()
  get placeholder() {
    return this._placeholder;
  }

  set placeholder(plh) {
    this._placeholder = plh;
    this.stateChanges.next();
  }

  private _placeholder: string;

  // From ControlValueAccessor interface
  focused: boolean = false;

  // From ControlValueAccessor interface
  get empty() {
    return isEmpty(this.value);
  }

  // From ControlValueAccessor interface
  @HostBinding('class.floating')
  get shouldLabelFloat() {
    return this.focused || !this.empty;
  }

  // From ControlValueAccessor interface
  @Input()
  get required() {
    return this._required;
  }

  set required(req) {
    this._required = coerceBooleanProperty(req);
    this.stateChanges.next();
  }

  private _required = false;

  // From ControlValueAccessor interface
  @Input()
  get disabled() {
    return this._disabled || (this.ngControl && this.ngControl.disabled);
  }

  set disabled(dis) {
    this._disabled = coerceBooleanProperty(dis);

    dis ? this.colorFormControl.disable({ emitEvent: false }) : this.colorFormControl.enable({ emitEvent: false });

    this.stateChanges.next();
  }

  private _disabled = false;

  // From ControlValueAccessor interface
  get errorState(): boolean {
    return (
      this.touched &&
      // if required check if is empty
      ((this.required && this.empty) ||
        // if there is a touched ngControl check if there is an error
        (this.ngControl && this.ngControl.touched && !!this.ngControl.errors) ||
        // if there is a touched colorFormControl check if there is an error
        (this.colorFormControl && this.colorFormControl.touched && !!this.colorFormControl.errors))
    );
  }

  // From ControlValueAccessor interface
  controlType?: string;

  // From ControlValueAccessor interface
  autofilled?: boolean;

  // From ControlValueAccessor interface
  userAriaDescribedBy?: string;

  private unsubscribe$ = new Subject<boolean>();

  constructor(
    // From ControlValueAccessor interface
    @Optional() @Self() public readonly ngControl: NgControl,
    private readonly elRef: ElementRef,
    private readonly fm: FocusMonitor,
  ) {
    // Replace the provider from above with this.
    if (this.ngControl != null) {
      // Setting the value accessor directly (instead of using
      // the providers) to avoid running into a circular import.
      this.ngControl.valueAccessor = this;
    }

    fm.monitor(elRef.nativeElement, true).subscribe((origin) => {
      this.focused = !!origin;
      this._onTouched();
      this.touched = true;
      this.stateChanges.next();
    });

    this.colorFormControl.valueChanges.pipe(debounceTime(300), takeUntil(this.unsubscribe$)).subscribe((value) => {
      this.value = value;

      this.colorFormControl.setValue(value, { onlySelf: true, emitEvent: false, emitModelToViewChange: true });
    });
  }

  ngDoCheck() {
    // sync control touched with local touched
    if (this.ngControl != null && this.touched !== this.ngControl.touched) {
      this.touched = this.ngControl.touched;

      this.stateChanges.next();
    }
  }

  ngOnDestroy() {
    this.stateChanges.complete();
    this.fm.stopMonitoring(this.elRef.nativeElement);
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  // From ControlValueAccessor interface
  writeValue(value: string): void {
    this._value = value;
    this._onChange(value);
    this.colorFormControl.setValue(value, { emitEvent: false });
  }

  // From ControlValueAccessor interface
  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor interface
  @HostBinding('attr.aria-describedby') describedBy = '';

  setDescribedByIds(ids: string[]): void {
    this.describedBy = ids.join(' ');
  }

  // From ControlValueAccessor interface

  onContainerClick(event: MouseEvent) {
    if ((event.target as Element).tagName.toLowerCase() !== 'input') {
      this.elRef.nativeElement.querySelector('input').focus();
    }
  }

  // From ControlValueAccessor interface
  setDisabledState(disabled: boolean) {
    this.disabled = disabled;
  }
}
