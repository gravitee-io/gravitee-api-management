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
  AsyncValidator,
  AsyncValidatorFn,
  ControlValueAccessor,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { isEmpty } from 'lodash';
import { filter, map, observeOn, startWith, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { asyncScheduler, Observable, of, Subject, timer } from 'rxjs';

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
  public listenerFormArray = new UntypedFormArray([this.newListenerFormGroup(DEFAULT_LISTENER)], [this.listenersValidator()]);
  public contextPathPrefix: string;
  public isDisabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();

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

  private initForm(): void {
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
      path: new UntypedFormControl(listener.path || '/', {
        validators: [this.validateGenericPathListenerControl()],
        asyncValidators: [this.listenersAsyncValidator()],
      }),
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

  public onDelete(pathIndex: number): void {
    this.listenerFormArray.removeAt(pathIndex);
    this._onTouched();
  }

  public listenersValidator(): ValidatorFn {
    return (formArray: UntypedFormArray): ValidationErrors | null => {
      const listenerFormArrayControls = formArray.controls;
      const listenerValues: string[] = listenerFormArrayControls.map((listener) => listener.value?.path);

      if (new Set(listenerValues).size !== listenerValues.length) {
        return { contextPath: 'Duplicated context path not allowed' };
      }
      return null;
    };
  }

  public validateGenericPathListenerControl(): ValidatorFn {
    return (formControl: UntypedFormControl): ValidationErrors | null => {
      const contextPath: string = formControl.value;
      if (isEmpty(contextPath)) {
        return {
          contextPath: 'Context path is required.',
        };
      } else if (contextPath.includes('//')) {
        return { contextPath: 'Context path is not valid.' };
      } else if (!PATH_PATTERN_REGEX.test(contextPath)) {
        return { contextPath: 'Context path is not valid.' };
      }
      return null;
    };
  }

  public listenersAsyncValidator(): AsyncValidatorFn {
    return (formControl: UntypedFormControl): Observable<ValidationErrors | null> => {
      if (formControl && formControl.dirty) {
        return timer(250).pipe(
          switchMap(() => this.apiV2Service.verifyPath(this.apiId, [{ path: formControl.value }])),
          map((res) => (res.ok ? null : { listeners: res.reason })),
        );
      }
      return of(null);
    };
  }

  protected getValue(): PathV4[] {
    return this.listenerFormArray?.controls.map((control) => {
      return { path: control.get('path').value };
    });
  }
}
