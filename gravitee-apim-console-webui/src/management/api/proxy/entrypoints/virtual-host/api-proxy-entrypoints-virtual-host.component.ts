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
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { Api } from '../../../../../entities/api';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-proxy-entrypoints-virtual-host',
  template: require('./api-proxy-entrypoints-virtual-host.component.html'),
  styles: [require('./api-proxy-entrypoints-virtual-host.component.scss')],
})
export class ApiProxyEntrypointsVirtualHostComponent implements OnChanges {
  @Input()
  apiProxy: Api['proxy'];

  @Output()
  public apiProxySubmit = new EventEmitter<Api['proxy']>();

  public entrypointsForm: FormGroup;
  public initialEntrypointsFormValue: unknown;

  constructor(private readonly permissionService: GioPermissionService) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiProxy) {
      this.initForm(this.apiProxy);
    }
  }

  onSubmit() {
    // TODO
  }

  private initForm(apiProxy: Api['proxy']) {
    // TODO
  }
}
