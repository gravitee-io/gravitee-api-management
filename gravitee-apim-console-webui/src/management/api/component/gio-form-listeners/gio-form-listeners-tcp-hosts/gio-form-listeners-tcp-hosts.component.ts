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
import { Component, ElementRef, forwardRef, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  AsyncValidator,
  ControlValueAccessor,
  FormArray,
  FormControl,
  FormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { isEmpty } from 'lodash';
import { filter, map, observeOn, startWith, take, takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { asyncScheduler, Observable, Subject } from 'rxjs';

import { TcpHost } from '../../../../../entities/management-api-v2/api/v4/tcpHost';

@Component({
  selector: 'gio-form-listeners-tcp-hosts',
  template: require('./gio-form-listeners-tcp-hosts.component.html'),
  styles: [require('../gio-form-listeners.common.scss')],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormListenersTcpHostsComponent),
      multi: true,
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => GioFormListenersTcpHostsComponent),
      multi: true,
    },
  ],
})
export class GioFormListenersTcpHostsComponent implements OnInit, OnDestroy, ControlValueAccessor, AsyncValidator {
  public listeners: TcpHost[] = [];
  public mainForm: FormGroup;
  public listenerFormArray = new FormArray([this.newListenerFormGroup({})], [this.listenersValidator()]);
  public isDisabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();

  public onDelete(hostIndex: number): void {
    this.listenerFormArray.controls.forEach((control) => {
      control.get('host').setErrors(null);
    });
    this.listenerFormArray.removeAt(hostIndex);
    this.listenerFormArray.updateValueAndValidity();
    this._onTouched();
  }

  protected _onChange: (_listeners: TcpHost[] | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(private readonly fm: FocusMonitor, private readonly elRef: ElementRef) {
    this.mainForm = new FormGroup({
      listeners: this.listenerFormArray,
    });
  }

  ngOnInit(): void {
    this.listenerFormArray?.valueChanges
      .pipe(
        tap((listeners) => listeners.length > 0 && this._onChange(listeners)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.fm
      .monitor(this.elRef.nativeElement, true)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this._onTouched();
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  // From ControlValueAccessor interface
  public writeValue(listeners: TcpHost[] | null = []): void {
    if (!listeners || isEmpty(listeners)) {
      return;
    }

    this.listeners = listeners;
    this.initForm();
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (listeners: TcpHost[] | null) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor interface
  public setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;

    isDisabled ? this.mainForm?.disable() : this.mainForm?.enable();
  }

  private initForm(): void {
    // Clear all previous hosts
    this.listenerFormArray.clear();

    // Populate hosts array from hosts
    this.listeners.forEach((listener) => {
      this.listenerFormArray.push(this.newListenerFormGroup(listener), {
        emitEvent: false,
      });
    });
    this.listenerFormArray.updateValueAndValidity();
  }

  public addEmptyListener() {
    this.listenerFormArray.push(this.newListenerFormGroup({}), { emitEvent: true });
  }

  public newListenerFormGroup(listener: TcpHost) {
    return new FormGroup({
      host: new FormControl(listener.host || ''),
    });
  }

  public validate(): Observable<ValidationErrors | null> {
    return this.listenerFormArray.statusChanges.pipe(
      observeOn(asyncScheduler),
      startWith(this.listenerFormArray.status),
      filter(() => !this.listenerFormArray.pending),
      map(() => (this.listenerFormArray.valid ? null : { invalid: true })),
      take(1),
    );
  }

  private listenersValidator(): ValidatorFn {
    return (listenerFormArrayControl: FormArray): ValidationErrors | null => {
      const listenerFormArrayControls = listenerFormArrayControl.controls;
      const listenerValues = listenerFormArrayControls.map((listener) => listener.value);

      const errors = listenerFormArrayControls
        .reduce((acc, listenerControl, index) => {
          const validationError = this.validateListenerControl(
            listenerControl,
            listenerValues.map((listener) => listener),
            index,
          );
          if (validationError) {
            acc[`${index}`] = validationError;
          }
          return acc;
        }, [])
        .filter((err) => err !== null && !isEmpty(err));

      return isEmpty(errors) ? null : errors;
    };
  }

  public validateListenerControl(listenerControl: AbstractControl, tcpListeners: TcpHost[], currentIndex: number): ValidationErrors | null {
    const listenerHostControl = listenerControl.get('host');
    let error = this.validateGenericHostListenerControl(listenerControl);
    if (!error) {
      const hostAlreadyExist = tcpListeners
        .filter((l, index) => index !== currentIndex)
        .map((l) => l.host)
        .includes(listenerHostControl.value);
      if (hostAlreadyExist) {
        error = { host: 'Host is already used.' };
      }
    }
    setTimeout(() => listenerHostControl.setErrors(error), 0);
    return error;
  }

  public validateGenericHostListenerControl(listenerControl: AbstractControl): ValidationErrors | null {
    const listenerHostControl = listenerControl.get('host');
    const host: string = listenerHostControl.value;

    let errors = null;
    if (isEmpty(host)) {
      errors = {
        host: 'Host is required.',
      };
    }
    setTimeout(() => listenerHostControl.setErrors(errors), 0);
    return errors;
  }

  protected getValue(): TcpHost[] {
    return this.listenerFormArray?.controls.map((control) => {
      return { host: control.get('host').value };
    });
  }
}
