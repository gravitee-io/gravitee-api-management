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
import { Component, ElementRef, forwardRef, Inject, Input, OnDestroy, OnInit } from "@angular/core";
import {
  AbstractControl,
  AsyncValidator,
  AsyncValidatorFn,
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
import { asyncScheduler, Observable, of, Subject, zip } from 'rxjs';

import { ApiService } from '../../../../../services-ngx/api.service';
import { PathV4 } from '../../../../../entities/management-api-v2';

const PATH_PATTERN_REGEX = new RegExp(/^\/[/.a-zA-Z0-9-_]*$/);
import { Constants } from '../../../../../entities/Constants';

const DEFAULT_LISTENER: PathV4 = {
  path: '/',
};

@Component({
  selector: 'gio-form-listeners-context-path',
  template: require('./gio-form-listeners-context-path.component.html'),
  styles: [require('../gio-form-listeners.common.scss')],
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
  public pathsToIgnore: PathV4[] = [];

  @Input()
  public apiId?: string;

  public listeners: PathV4[] = [DEFAULT_LISTENER];
  public mainForm: FormGroup;
  public listenerFormArray = new FormArray(
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
    protected readonly apiService: ApiService,
    @Inject(Constants) public readonly constants: Constants,
  ) {
    this.mainForm = new FormGroup({
      listeners: this.listenerFormArray,
    });
  }

  ngOnInit(): void {
    this.contextPathPrefix = this.constants.env.settings.portal.entrypoint;
    if (this.contextPathPrefix.endsWith('/')) {
      this.contextPathPrefix = this.contextPathPrefix.slice(0, -1);
    }

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
    return new FormGroup({
      path: new FormControl(listener.path || '/'),
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
    let error = this.validateContextPath(listenerControl);
    if (!error) {
      const contextPathAlreadyExist = httpListeners
        .filter((l, index) => index !== currentIndex)
        .map((l) => l.path)
        .includes(listenerPathControl.value);
      if (contextPathAlreadyExist) {
        error = { contextPath: 'Context path is already use.' };
      }
    }
    setTimeout(() => listenerPathControl.setErrors(error), 0);
    return error;
  }

  private validateContextPath(listenerControl: AbstractControl): ValidationErrors | null {
    const listenerPathControl = listenerControl.get('path');
    const contextPath: string = listenerPathControl.value;

    let errors = this.validateGenericPathListenerControl(contextPath);

    if (contextPath.length < 3) {
      errors = { contextPath: 'Context path has to be more than 3 characters long.' };
    }

    setTimeout(() => listenerPathControl.setErrors(errors), 0);
    return errors;
  }

  protected validateGenericPathListenerControl(contextPath: string): ValidationErrors | null {
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
    return errors;
  }

  protected listenersAsyncValidator(): AsyncValidatorFn {
    return (listenerFormArrayControl: FormArray): Observable<ValidationErrors | null> => {
      const listenerFormArrayControls = listenerFormArrayControl.controls;

      const contextPathsToIgnore = this.pathsToIgnore?.map((p) => p.path) ?? [];
      const pathValidations$: Observable<ValidationErrors | null>[] = listenerFormArrayControls.map((listenerControl) => {
        const listenerPathControl = listenerControl.get('path');
        const contextPathValue = listenerPathControl.value;
        if (contextPathsToIgnore.includes(contextPathValue)) {
          return of(null);
        }
        return this.apiService.verify({ contextPath: contextPathValue }, this.apiId);
      });

      return zip(...pathValidations$).pipe(
        map((errors: (ValidationErrors | null)[]) => {
          errors.forEach((error, index) => {
            setTimeout(() => listenerFormArrayControls.at(index).get('path').setErrors(error), 0);
          });
          if (errors.filter((v) => v !== null).length === 0) {
            return null;
          }
          return { listeners: true };
        }),
      );
    };
  }
}
