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
import { AbstractControl, FormArray, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { escapeRegExp, isEmpty } from 'lodash';

import { Proxy } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-proxy-entrypoints-virtual-host',
  template: require('./api-proxy-entrypoints-virtual-host.component.html'),
  styles: [require('./api-proxy-entrypoints-virtual-host.component.scss')],
})
export class ApiProxyEntrypointsVirtualHostComponent implements OnChanges {
  @Input()
  readOnly: boolean;

  @Input()
  apiProxy: Proxy;

  @Input()
  domainRestrictions: string[] = [];

  @Output()
  public apiProxySubmit = new EventEmitter<Proxy>();

  public virtualHostsForm: FormGroup;
  private get virtualHostsFormArray(): FormArray {
    return this.virtualHostsForm.get('virtualHosts') as FormArray;
  }

  public get virtualHostsTableData(): unknown[] {
    // Create new array to trigger change detection
    return [...(this.virtualHostsFormArray?.controls ?? [])];
  }
  public virtualHostsTableDisplayedColumns = ['host', 'path', 'overrideEntrypoint', 'remove'];

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiProxy || changes.readOnly) {
      this.initForm(this.apiProxy);
    }
  }

  onSubmit() {
    const virtualHosts: Proxy['virtualHosts'] = this.virtualHostsFormArray.getRawValue().map((virtualHost) => ({
      host: combineHostWithDomain(virtualHost.host, virtualHost.hostDomain),
      path: virtualHost.path,
      overrideEntrypoint: virtualHost.overrideEntrypoint,
    }));

    this.apiProxySubmit.emit({
      ...this.apiProxy,
      virtualHosts: virtualHosts,
    });
  }

  onAddVirtualHost() {
    this.virtualHostsFormArray.push(this.newVirtualHostFormGroup());
    this.virtualHostsFormArray.markAsDirty();
  }

  onDeleteVirtualHostClicked(index: number) {
    this.virtualHostsFormArray.removeAt(index);
    this.virtualHostsFormArray.markAsDirty();
  }

  onResetClicked() {
    // Needed to re init all form controls
    this.initForm(this.apiProxy);
  }

  private initForm(apiProxy: Proxy) {
    // Wrap form array in a form group because angular doesn't support form array as root form control
    this.virtualHostsForm = new FormGroup({
      virtualHosts: new FormArray([...apiProxy.virtualHosts.map((virtualHost) => this.newVirtualHostFormGroup(virtualHost))]),
    });
  }

  private newVirtualHostFormGroup(virtualHost?: Proxy['virtualHosts'][number]) {
    const hostObj = extractDomainToHost(virtualHost?.host, this.domainRestrictions);

    return new FormGroup({
      host: new FormControl(
        {
          value: hostObj.host ?? '',
          disabled: this.readOnly,
        },
        [hostValidator(this.domainRestrictions)],
      ),
      hostDomain: new FormControl(
        {
          value: hostObj.hostDomain ?? '',
          disabled: this.readOnly,
        },
        [hostValidator(this.domainRestrictions)],
      ),
      path: new FormControl(
        {
          value: virtualHost?.path ?? '',
          disabled: this.readOnly,
        },
        [Validators.required, Validators.pattern(/^\/[/.a-zA-Z0-9-_]*$/)],
      ),
      overrideEntrypoint: new FormControl({
        value: virtualHost?.overrideEntrypoint ?? false,
        disabled: this.readOnly,
      }),
    });
  }
}

// Common validator for host and hostDomain
const hostValidator = (domainRestrictions: string[] = []): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.parent) {
      return null;
    }

    const hostControl = control.parent.get('host');
    const domainControl = control.parent.get('hostDomain');

    const fullHost = hostControl?.value + domainControl?.value;

    if (!isEmpty(domainRestrictions)) {
      const isValid = domainRestrictions.some((domainRestriction) => fullHost.endsWith(domainRestriction));
      const errors = isValid ? null : { host: 'true' };
      hostControl.setErrors(errors);
      return errors;
    }
    hostControl.setErrors(null);
    domainControl.setErrors(null);
    return null;
  };
};

const extractDomainToHost = (fullHost: string, domainRestrictions: string[] = []): { host: string; hostDomain: string } => {
  let host = fullHost;
  let hostDomain = '';

  if (!isEmpty(domainRestrictions)) {
    hostDomain = fullHost && domainRestrictions.find((domain) => fullHost.endsWith(`${domain}`));

    if (hostDomain) {
      host = fullHost.replace(new RegExp(`\\.?${escapeRegExp(hostDomain)}$`), '');
    }
  }

  return { host, hostDomain };
};

const combineHostWithDomain = (host: string, hostDomain: string): string => {
  if (isEmpty(hostDomain)) {
    return host;
  }
  if (isEmpty(host)) {
    return hostDomain;
  }
  return `${host}.${hostDomain}`;
};
