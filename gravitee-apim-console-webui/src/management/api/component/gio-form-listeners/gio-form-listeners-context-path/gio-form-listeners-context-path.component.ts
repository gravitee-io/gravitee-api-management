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
import { filter, map, observeOn, startWith, take, takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { asyncScheduler, Observable, Subject } from 'rxjs';

import { PathV4 } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { contextPathModePathSyncValidator } from '../../../../../shared/validators/context-path/context-path-sync-validator.directive';
import { contextPathAsyncValidator } from '../../../../../shared/validators/context-path/context-path-async-validator.directive';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';

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
  public listenerFormArray: UntypedFormArray;
  public contextPathPrefix: string;
  public isDisabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();

  protected _onChange: (_listeners: PathV4[] | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
    protected readonly apiV2Service: ApiV2Service,
    private readonly environmentSettingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit(): void {
    this.listenerFormArray = new UntypedFormArray([this.newListenerFormGroup(DEFAULT_LISTENER)], [this.listenersValidator()]);

    this.mainForm = new UntypedFormGroup({
      listeners: this.listenerFormArray,
    });

    this.environmentSettingsService
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
        validators: [contextPathModePathSyncValidator],
        asyncValidators: [contextPathAsyncValidator(this.apiV2Service, this.apiId)],
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

  protected getValue(): PathV4[] {
    return this.listenerFormArray?.controls.map((control) => {
      return { path: control.get('path').value };
    });
  }
}
