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
import { Component, forwardRef } from '@angular/core';
import { FormControl, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';

import { GioFormListenersContextPathComponent } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.component';
import { HttpListenerPath } from '../../../entities/api-v4';

@Component({
  selector: 'gio-form-listeners-virtual-host',
  template: require('./gio-form-listeners-virtual-host.component.html'),
  styles: [require('../gio-form-listeners.common.scss')],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormListenersVirtualHostComponent),
      multi: true,
    },
  ],
})
export class GioFormListenersVirtualHostComponent extends GioFormListenersContextPathComponent {
  public newListenerFormGroup(listener: HttpListenerPath): FormGroup {
    return new FormGroup({
      host: new FormControl(listener.host || ''),
      path: new FormControl(listener.path || '', [Validators.required], [this.apiService.contextPathValidator()]),
      overrideAccess: new FormControl(listener.overrideAccess || false),
    });
  }
}
