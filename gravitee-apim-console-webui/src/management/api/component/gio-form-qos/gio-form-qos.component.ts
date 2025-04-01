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
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject } from 'rxjs';
import { isEmpty } from 'lodash';

import { Qos } from '../../../../entities/management-api-v2';

@Component({
  selector: 'gio-form-qos',
  templateUrl: './gio-form-qos.component.html',
  styleUrls: ['./gio-form-qos.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormQosComponent),
      multi: true,
    },
  ],
})
export class GioFormQosComponent implements OnDestroy, ControlValueAccessor {
  private unsubscribe$: Subject<void> = new Subject<void>();
  @Input()
  public id: string;

  @Input()
  public supportedQos?: Qos[];

  public _selectedQos: Qos;

  get selectedQos() {
    return this._selectedQos;
  }
  set selectedQos(selection: Qos) {
    this._selectedQos = selection;
    this._onTouched();
    this._onChange(selection);
  }
  protected _onChange: (_selection: Qos | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  // From ControlValueAccessor interface
  public writeValue(selection: Qos | null): void {
    if (!selection || isEmpty(selection)) {
      return;
    }

    this.selectedQos = selection;
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (selection: Qos | null) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }
}
