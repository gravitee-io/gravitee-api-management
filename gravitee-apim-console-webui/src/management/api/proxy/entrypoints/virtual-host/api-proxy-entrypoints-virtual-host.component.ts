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
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';

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

  @Input()
  domainRestrictions: string[] = [];

  @Output()
  public apiProxySubmit = new EventEmitter<Api['proxy']>();

  public virtualHostsFormArray: FormArray;
  
  public get virtualHostsTableData(): unknown[] {
    // Create new array to trigger change detection
    return [...this.virtualHostsFormArray?.controls] ?? []
  }
  public virtualHostsTableDisplayedColumns = ['host', 'path', 'override_entrypoint', 'remove'];
  public initialVirtualHostsFormValue: unknown;

  public hostPattern: string;

  constructor(private readonly permissionService: GioPermissionService) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiProxy) {
      this.initForm(this.apiProxy);
    }
    if (changes.domainRestrictions) {
      // TODO
    }
  }

  onSubmit() {
    const virtualHosts: Api['proxy']['virtual_hosts'] = this.virtualHostsFormArray.getRawValue().map((virtualHost) => ({
      host: virtualHost.host,
      path: virtualHost.path,
      override_entrypoint: virtualHost.override_entrypoint,
    }));

    this.apiProxySubmit.emit({
      ...this.apiProxy,
      virtual_hosts: virtualHosts,
    });
  }

  onAddVirtualHost() {
    this.virtualHostsFormArray.push(this.newVirtualHostFormGroup());
    this.virtualHostsFormArray.markAsDirty();
  }

  onDeleteVirtualHostClicked( index: number) {
    this.virtualHostsFormArray.removeAt(index);
    this.virtualHostsFormArray.markAsDirty();
  }

  private initForm(apiProxy: Api['proxy']) {
    this.virtualHostsFormArray = new FormArray([...apiProxy.virtual_hosts.map((virtualHost) => this.newVirtualHostFormGroup(virtualHost))]);

    this.initialVirtualHostsFormValue = this.virtualHostsFormArray.getRawValue();
  }

  private newVirtualHostFormGroup(virtualHost?: Api['proxy']['virtual_hosts'][number]) {
    return new FormGroup({
      host: new FormControl(
        {
          value: virtualHost?.host ?? '',
          disabled: !this.permissionService.hasAnyMatching(['api-definition-u', 'api-gateway_definition-u']),
        },
        [Validators.required],
      ),
      path: new FormControl(
        {
          value: virtualHost?.path ?? '',
          disabled: !this.permissionService.hasAnyMatching(['api-definition-u', 'api-gateway_definition-u']),
        },
        [Validators.required, Validators.pattern(/^\/[/.a-zA-Z0-9-_]*$/)],
      ),
      override_entrypoint: new FormControl({
        value: virtualHost?.override_entrypoint ?? false,
        disabled: !this.permissionService.hasAnyMatching(['api-definition-u', 'api-gateway_definition-u']),
      }),
    });
  }
}
