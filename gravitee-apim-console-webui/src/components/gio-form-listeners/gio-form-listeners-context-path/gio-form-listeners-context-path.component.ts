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
import { ControlValueAccessor, FormArray, FormControl, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { dropRight, isEmpty } from 'lodash';
import { takeUntil, tap } from 'rxjs/operators';
import { FocusMonitor } from '@angular/cdk/a11y';
import { Subject } from 'rxjs';

import { HttpListenerPath } from '../../../entities/api-v4';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { ApiService } from '../../../services-ngx/api.service';

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
  ],
})
export class GioFormListenersContextPathComponent implements OnInit, OnDestroy, ControlValueAccessor {
  public listeners: HttpListenerPath[] = [];
  public mainForm: FormGroup;
  public listenerFormArray = new FormArray([this.newListenerFormGroup({})]);
  public contextPathPrefix: string;
  private unsubscribe$: Subject<void> = new Subject<void>();

  public onDelete(pathIndex: number): void {
    this._onTouched();
    this.listenerFormArray.removeAt(pathIndex);
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
          if (
            listeners.length > 0 &&
            !this.listenerFormArray.controls[listeners.length - 1].invalid &&
            !this.isEmpty(listeners[listeners.length - 1])
          ) {
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
      path: new FormControl(
        listener.path || '',
        [Validators.required, Validators.pattern(/^\/[/.a-zA-Z0-9-_]*$/)],
        [this.apiService.contextPathValidator()],
      ),
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
}
