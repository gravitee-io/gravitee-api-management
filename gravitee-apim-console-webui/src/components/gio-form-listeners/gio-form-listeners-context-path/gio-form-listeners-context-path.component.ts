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
import { dropRight, isEmpty } from 'lodash';
import { filter, map, startWith, take, takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { Observable, of, Subject, zip } from 'rxjs';

import { HttpListenerPath } from '../../../entities/api-v4';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { ApiService } from '../../../services-ngx/api.service';

const PATH_PATTERN_REGEX = new RegExp(/^\/[/.a-zA-Z0-9-_]*$/);

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
  public listeners: HttpListenerPath[] = [];
  public mainForm: FormGroup;
  public listenerFormArray = new FormArray([this.newListenerFormGroup({})], [this.listenersValidator()], [this.listenersAsyncValidator()]);
  public contextPathPrefix: string;
  private unsubscribe$: Subject<void> = new Subject<void>();
  private verifiedPath: Record<string, ValidationErrors | null> = {};

  public onDelete(pathIndex: number): void {
    this.listenerFormArray.controls.forEach((control) => {
      control.get('path').setErrors(null);
    });
    this.listenerFormArray.removeAt(pathIndex);
    this.listenerFormArray.updateValueAndValidity();
    this._onTouched();
  }

  private _onChange: (_listeners: HttpListenerPath[] | null) => void = () => ({});

  private _onTouched: () => void = () => ({});

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
    protected readonly apiService: ApiService,
    private readonly portalSettingsService: PortalSettingsService,
  ) {
    this.mainForm = new FormGroup({
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

    // When user start to complete last listener add new empty one at the end
    this.listenerFormArray?.valueChanges
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((listeners) => listeners.length > 0 && this._onChange(this.onChange(listeners))),
        tap((listeners: HttpListenerPath[]) => {
          if (listeners.length > 0 && !this.isEmpty(listeners[listeners.length - 1])) {
            this.addEmptyListener();
          }
        }),
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
  public writeValue(listeners: HttpListenerPath[] | null): void {
    if (!listeners) {
      return;
    }

    this.listeners = listeners ?? [];
    this.initForm();
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (listeners: HttpListenerPath[] | null) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
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

    // add one empty path at the end
    this.addEmptyListener();
  }

  public addEmptyListener() {
    this.listenerFormArray.push(this.newListenerFormGroup({}), { emitEvent: false });
  }

  public newListenerFormGroup(listener: HttpListenerPath) {
    return new FormGroup({
      path: new FormControl(listener.path || ''),
    });
  }

  public onChange(listeners: HttpListenerPath[]) {
    const lastListener = listeners[listeners.length - 1];
    if (lastListener && this.isEmpty(lastListener)) {
      return dropRight(listeners);
    }
    return listeners;
  }

  public isEmpty(lastListener: HttpListenerPath) {
    return isEmpty(lastListener.path);
  }

  public validate(): Observable<ValidationErrors | null> {
    return this.listenerFormArray.statusChanges.pipe(
      startWith(this.listenerFormArray.status),
      filter(() => !this.listenerFormArray.pending),
      map(() => (this.listenerFormArray.valid ? null : { invalid: true })),
      take(1),
    );
  }

  private listenersValidator(): ValidatorFn {
    return (listenerFormArrayControl: FormArray): ValidationErrors | null => {
      if (!listenerFormArrayControl.dirty) {
        return null;
      }
      const listenerFormArrayControls = listenerFormArrayControl.controls;
      const ignoreLast = this.mustIgnoreLast(listenerFormArrayControls);
      const listenerValues = listenerFormArrayControls.map((listener) => listener.value);

      const errors = listenerFormArrayControls.reduce((acc, listenerControl, index) => {
        if (!(ignoreLast && index === listenerFormArrayControls.length - 1)) {
          const validationError = this.validateListenerControl(listenerControl, listenerValues, index);
          if (validationError) {
            acc[`${index}`] = validationError;
          }
        }
        return acc;
      }, {});

      return isEmpty(errors) ? null : errors;
    };
  }

  private mustIgnoreLast(listenerFormArrayControls: AbstractControl[]) {
    if (listenerFormArrayControls.length > 1) {
      // Remove last if is empty
      const lastFormArrayControl = listenerFormArrayControls.at(listenerFormArrayControls.length - 1);
      return isEmpty(lastFormArrayControl.get('path').value);
    }
    return false;
  }

  public validateListenerControl(
    listenerControl: AbstractControl,
    httpListeners: HttpListenerPath[],
    currentIndex: number,
  ): ValidationErrors | null {
    const listenerPathControl = listenerControl.get('path');
    const contextPath = listenerPathControl.value;

    let errors = null;
    if (isEmpty(contextPath)) {
      errors = {
        contextPath: 'Context path is required.',
      };
    } else if (contextPath.length < 3) {
      errors = { contextPath: 'Context path has to be more than 3 characters long.' };
    } else if (!PATH_PATTERN_REGEX.test(contextPath)) {
      errors = { contextPath: 'Context path is not valid.' };
    } else if (httpListeners.find((httpListener, index) => index !== currentIndex && httpListener.path === contextPath) != null) {
      errors = { contextPath: 'Context path is already use.' };
    }
    setTimeout(() => listenerPathControl.setErrors(errors), 0);
    return errors;
  }

  private listenersAsyncValidator(): AsyncValidatorFn {
    return (listenerFormArrayControl: FormArray): Observable<ValidationErrors | null> => {
      if (!listenerFormArrayControl.dirty) {
        return of(null);
      }
      const listenerFormArrayControls = listenerFormArrayControl.controls;
      const ignoreLast = this.mustIgnoreLast(listenerFormArrayControls);

      const foobar: Observable<ValidationErrors | null>[] = listenerFormArrayControls.map((listenerControl, index) => {
        if (ignoreLast && index === listenerFormArrayControls.length - 1) {
          return of(null);
        }
        const listenerPathControl = listenerControl.get('path');
        return this.apiService.verify(listenerPathControl.value);
      });

      return zip(...foobar).pipe(
        map((errors: (ValidationErrors | null)[]) => {
          errors.forEach((error, index) => {
            listenerFormArrayControls.at(index).get('path').setErrors(error);
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
