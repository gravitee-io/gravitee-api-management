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
import { UIRouterGlobals } from '@uirouter/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { Api } from '../../../../../entities/api';
import { ApiService } from '../../../../../services-ngx/api.service';

@Component({
  selector: 'api-proxy-entrypoints-context-path',
  template: require('./api-proxy-entrypoints-context-path.component.html'),
  styles: [require('./api-proxy-entrypoints-context-path.component.scss')],
})
export class ApiProxyEntrypointsContextPathComponent implements OnChanges {
  @Input()
  readOnly: boolean;

  @Input()
  apiProxy: Api['proxy'];

  @Output()
  public apiProxySubmit = new EventEmitter<Api['proxy']>();

  public entrypointsForm: FormGroup;
  public initialEntrypointsFormValue: unknown;

  constructor(private readonly apiService: ApiService, private readonly $router: UIRouterGlobals) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiProxy || changes.readOnly) {
      this.initForm(this.apiProxy);
    }
  }

  onSubmit() {
    this.apiProxySubmit.emit({ ...this.apiProxy, virtual_hosts: [{ path: this.entrypointsForm.value.contextPath }] });
  }

  private initForm(apiProxy: Api['proxy']) {
    const currentContextPath = apiProxy.virtual_hosts[0].path;
    const { apiId } = this.$router.params;

    this.entrypointsForm = new FormGroup({
      contextPath: new FormControl(
        {
          value: apiProxy.virtual_hosts[0].path,
          disabled: this.readOnly,
        },
        [Validators.required],
        [
          this.apiService.contextPathValidator({
            currentContextPath,
            apiId,
          }),
        ],
      ),
    });
    this.initialEntrypointsFormValue = this.entrypointsForm.getRawValue();
  }
}
