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
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { hostSyncValidator } from '../../../../../shared/validators/host/host-sync-validator.directive';
import { hostAsyncValidator } from '../../../../../shared/validators/host/host-async-validator.directive';

@Component({
  selector: 'gio-form-listeners-tcp-hosts',
  templateUrl: './gio-form-listeners-tcp-hosts.component.html',
  styleUrls: ['../gio-form-listeners.common.scss'],
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
  @Input()
  public apiId?: string;

  public listeners: TcpHost[] = [];
  public mainForm: FormGroup;
  public listenerFormArray: FormArray;
  public isDisabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();

  protected _onChange: (_listeners: TcpHost[] | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(
    private readonly fm: FocusMonitor,
    private readonly elRef: ElementRef,
    private readonly apiV2Service: ApiV2Service,
  ) {}

  ngOnInit(): void {
    this.listenerFormArray = new FormArray([this.newListenerFormGroup({})], { validators: [this.listenersValidator()] });
    this.mainForm = new FormGroup({
      listeners: this.listenerFormArray,
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

  public onDelete(hostIndex: number): void {
    this.listenerFormArray.removeAt(hostIndex);
    this._onTouched();
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
      host: new FormControl(listener.host || '', {
        validators: [hostSyncValidator],
        asyncValidators: [hostAsyncValidator(this.apiV2Service, this.apiId)],
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

  protected getValue(): TcpHost[] {
    return this.listenerFormArray?.controls.map((control) => {
      return { host: control.get('host').value };
    });
  }

  private listenersValidator(): ValidatorFn {
    return (formArray: FormArray): ValidationErrors | null => {
      const listenerFormArrayControls = formArray.controls;
      const listenerValues: string[] = listenerFormArrayControls.map((listener) => listener.value?.host);

      if (new Set(listenerValues).size !== listenerValues.length) {
        return { host: 'Duplicated hosts not allowed' };
      }
      return null;
    };
  }
}
