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
import { Component, forwardRef, Input } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, NG_ASYNC_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors } from '@angular/forms';
import { escapeRegExp, isEmpty } from 'lodash';

import { GioFormListenersContextPathComponent } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.component';
import { HttpListenerPath } from '../../../entities/api-v4';

interface InternalHttpListenerPath extends HttpListenerPath {
  _hostSubDomain?: string;
  _hostDomain?: string;
}

@Component({
  selector: 'gio-form-listeners-virtual-host',
  template: require('./gio-form-listeners-virtual-host.component.html'),
  styles: [require('../gio-form-listeners.common.scss'), require('./gio-form-listeners-virtual-host.component.scss')],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioFormListenersVirtualHostComponent),
      multi: true,
    },
    {
      provide: NG_ASYNC_VALIDATORS,
      useExisting: forwardRef(() => GioFormListenersVirtualHostComponent),
      multi: true,
    },
  ],
})
export class GioFormListenersVirtualHostComponent extends GioFormListenersContextPathComponent {
  @Input()
  public domainRestrictions: string[] = [];

  public newListenerFormGroup(listener: HttpListenerPath): FormGroup {
    const { host, hostDomain } = extractDomainToHost(listener?.host, this.domainRestrictions);

    return new FormGroup({
      host: new FormControl(listener?.host || ''),
      // Private controls for internal process
      _hostSubDomain: new FormControl(host || ''),
      _hostDomain: new FormControl(hostDomain || ''),
      path: new FormControl(listener.path),
      overrideAccess: new FormControl(listener.overrideAccess || false),
    });
  }

  public onChange(listeners: InternalHttpListenerPath[]): InternalHttpListenerPath[] {
    const changedListeners = super.onChange(listeners) as InternalHttpListenerPath[];
    return changedListeners.map((listener) => ({
      ...listener,
      host: combineSubDomainWithDomain(listener._hostSubDomain, listener._hostDomain),
    }));
  }

  validateListenerControl(
    listenerControl: AbstractControl,
    httpListeners: HttpListenerPath[],
    currentIndex: number,
  ): ValidationErrors | null {
    const inheritErrors = super.validateListenerControl(listenerControl, httpListeners, currentIndex);
    const subDomainControl = listenerControl.get('_hostSubDomain');
    const domainControl = listenerControl.get('_hostDomain');

    const fullHost = subDomainControl.value + domainControl.value;

    // When no domain restriction, host is required
    if (isEmpty(this.domainRestrictions) && isEmpty(subDomainControl.value)) {
      const errors = { host: 'Host is required.' };
      setTimeout(() => subDomainControl.setErrors(errors), 0);
      return { ...inheritErrors, ...errors };
    }

    if (!isEmpty(this.domainRestrictions)) {
      const isValid = this.domainRestrictions.some((domainRestriction) => fullHost.endsWith(domainRestriction));
      const errors = isValid ? null : { host: 'Host is not valid (must end with one of restriction domain).' };
      setTimeout(() => subDomainControl.setErrors(errors), 0);
      return { ...inheritErrors, ...errors };
    }
    setTimeout(() => subDomainControl.setErrors(null), 0);
    return inheritErrors;
  }
}

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

const combineSubDomainWithDomain = (hostSubDomain: string, hostDomain: string): string => {
  if (isEmpty(hostDomain)) {
    return hostSubDomain;
  }
  if (isEmpty(hostSubDomain)) {
    return hostDomain;
  }
  return `${hostSubDomain}.${hostDomain}`;
};
