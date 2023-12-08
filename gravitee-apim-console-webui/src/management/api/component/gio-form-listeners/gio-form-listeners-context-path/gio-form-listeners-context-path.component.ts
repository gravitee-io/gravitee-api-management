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
import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  AsyncValidator,
  AsyncValidatorFn,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormControl,
  UntypedFormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { isEmpty } from 'lodash';
import { filter, map, observeOn, startWith, take, takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { asyncScheduler, Observable, of, Subject } from 'rxjs';

import { PortalSettingsService } from '../../../../../services-ngx/portal-settings.service';
import { PathV4 } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';

const PATH_PATTERN_REGEX = new RegExp(/^\/[/.a-zA-Z0-9-_]*$/);

const DEFAULT_LISTENER: PathV4 = {
  path: '/',
};

@Component({
  selector: 'gio-form-listeners-context-path',
  templateUrl: './gio-form-listeners-context-path.component.html',
  styleUrls: ['../gio-form-listeners.common.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormListenersContextPathComponent),
      multi: true,
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => GioFormListenersContextPathComponent),
      multi: true,
    },
  ],
})
export class GioFormListenersContextPathComponent implements OnInit, OnDestroy, ControlValueAccessor, AsyncValidator {
  @Input()
  public apiId?: string;

  public listeners: PathV4[] = [DEFAULT_LISTENER];
  public mainForm: UntypedFormGroup;
  public listenerFormArray = new UntypedFormArray(
    [this.newListenerFormGroup(DEFAULT_LISTENER)],
    [this.listenersValidator()],
    [this.listenersAsyncValidator()],
  );
  public contextPathPrefix: string;
  public isDisabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();

  public onDelete(pathIndex: number): void {
    this.listenerFormArray.controls.forEach((control) => {
      control.get('path').setErrors(null);
    });
    this.listenerFormArray.removeAt(pathIndex);
    this.listenerFormArray.updateValueAndValidity();
    this._onTouched();
  }

  protected _onChange: (_listeners: PathV4[] | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
    protected readonly apiV2Service: ApiV2Service,
    private readonly portalSettingsService: PortalSettingsService,
  ) {
    this.mainForm = new UntypedFormGroup({
      listeners: this.listenerFormArray,
    });
  }

  ngOnInit(): void {
    this.portalSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings) => {
        this.contextPathPrefix = settings.portal.entrypoint.endsWith('/')
          ? settings.portal.entrypoint.slice(0, -1)
          : settings.portal.entrypoint;
      });

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
  public writeValue(listeners: PathV4[] | null): void {
    if (!listeners || isEmpty(listeners)) {
      return;
    }

    this.listeners = listeners;
    this.initForm();
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (listeners: PathV4[] | null) => void): void {
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

  public initForm(): void {
    // Clear all previous paths
    this.listenerFormArray.clear();

    // Populate paths array from paths
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

  public newListenerFormGroup(listener: PathV4) {
    return new UntypedFormGroup({
      path: new UntypedFormControl(listener.path || '/'),
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
    return (listenerFormArrayControl: UntypedFormArray): ValidationErrors | null => {
      const listenerFormArrayControls = listenerFormArrayControl.controls;
      const listenerValues = listenerFormArrayControls.map((listener) => listener.value);

      const errors = listenerFormArrayControls
        .reduce((acc, listenerControl, index) => {
          const validationError = this.validateListenerControl(listenerControl, listenerValues, index);
          if (validationError) {
            acc[`${index}`] = validationError;
          }
          return acc;
        }, [])
        .filter((err) => err !== null && !isEmpty(err));

      return isEmpty(errors) ? null : errors;
    };
  }

  public validateListenerControl(listenerControl: AbstractControl, httpListeners: PathV4[], currentIndex: number): ValidationErrors | null {
    const listenerPathControl = listenerControl.get('path');
    let error = this.validateGenericPathListenerControl(listenerControl);
    if (!error) {
      const contextPathAlreadyExist = httpListeners
        .filter((l, index) => index !== currentIndex)
        .map((l) => l.path)
        .includes(listenerPathControl.value);
      if (contextPathAlreadyExist) {
        error = { contextPath: 'Context path is already used.' };
      }
    }
    setTimeout(() => listenerPathControl.setErrors(error), 0);
    return error;
  }

  public validateGenericPathListenerControl(listenerControl: AbstractControl): ValidationErrors | null {
    const listenerPathControl = listenerControl.get('path');
    const contextPath: string = listenerPathControl.value;

    let errors = null;
    if (isEmpty(contextPath)) {
      errors = {
        contextPath: 'Context path is required.',
      };
    } else if (contextPath.includes('//')) {
      errors = { contextPath: 'Context path is not valid.' };
    } else if (!PATH_PATTERN_REGEX.test(contextPath)) {
      errors = { contextPath: 'Context path is not valid.' };
    }
    setTimeout(() => listenerPathControl.setErrors(errors), 0);
    return errors;
  }

  private listenersAsyncValidator(): AsyncValidatorFn {
    return (listenerFormArrayControl: UntypedFormArray): Observable<ValidationErrors | null> => {
      if (listenerFormArrayControl) {
        return this.apiV2Service
          .verifyPath(
            this.apiId,
            this.getValue()?.map((v) => {
              return { host: v.host, path: v.path };
            }),
          )
          .pipe(
            map((res) => {
              if (res.ok) {
                // Clear error
                setTimeout(() => this.mainForm.setErrors(null), 0);
                return null;
              } else {
                const error = { listeners: res.reason };
                this.mainForm.setErrors(error);
                return of(error);
              }
            }),
          );
      }

      return null;
    };
  }

  protected getValue(): PathV4[] {
    return this.listenerFormArray?.controls.map((control) => {
      return { path: control.get('path').value };
    });
  }
}
