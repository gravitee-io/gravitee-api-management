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
import {
  AbstractControl,
  UntypedFormControl,
  UntypedFormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
} from '@angular/forms';
import { escapeRegExp, isEmpty } from 'lodash';

import { GioFormListenersContextPathComponent } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.component';
import { PathV4 } from '../../../../../entities/management-api-v2';

interface InternalPathV4 extends PathV4 {
  _hostSubDomain?: string;
  _hostDomain?: string;
}

@Component({
  selector: 'gio-form-listeners-virtual-host',
  templateUrl: './gio-form-listeners-virtual-host.component.html',
  styleUrls: ['../gio-form-listeners.common.scss', './gio-form-listeners-virtual-host.component.scss'],
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

  public newListenerFormGroup(listener: PathV4): UntypedFormGroup {
    const { host, hostDomain } = extractDomainToHost(listener?.host, this.domainRestrictions);

    return new UntypedFormGroup({
      host: new UntypedFormControl(listener?.host || ''),
      // Private controls for internal process
      _hostSubDomain: new UntypedFormControl(host || ''),
      _hostDomain: new UntypedFormControl(hostDomain || ''),
      path: new UntypedFormControl(listener.path),
      overrideAccess: new UntypedFormControl(listener.overrideAccess || false),
    });
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (listeners: PathV4[] | null) => void): void {
    this._onChange = (listeners: InternalPathV4[]) =>
      fn(
        listeners.map((listener) => ({
          ...listener,
          host: combineSubDomainWithDomain(listener._hostSubDomain, listener._hostDomain),
        })),
      );
  }

  validateListenerControl(listenerControl: AbstractControl, httpListeners: PathV4[], currentIndex: number): ValidationErrors | null {
    const inheritErrors = super.validateGenericPathListenerControl(listenerControl);
    const subDomainControl = listenerControl.get('_hostSubDomain');
    const domainControl = listenerControl.get('_hostDomain');
    const contextPathControl = listenerControl.get('path');
    const contextPath = contextPathControl.value;
    const fullHost = combineSubDomainWithDomain(subDomainControl.value, domainControl.value);

    // When no domain restrictions, host is required
    if (isEmpty(this.domainRestrictions) && isEmpty(subDomainControl.value)) {
      const errors = { host: 'Host is required.' };
      setTimeout(() => subDomainControl.setErrors(errors), 0);
      return { ...inheritErrors, ...errors };
    }

    if (!isEmpty(this.domainRestrictions) && !this.domainRestrictions.some((domainRestriction) => fullHost.endsWith(domainRestriction))) {
      const errors = { host: 'Host is not valid (must end with one of restriction domain).' };
      setTimeout(() => subDomainControl.setErrors(errors), 0);
      return { ...inheritErrors, ...errors };
    }

    // Check host is not already defined
    if (
      httpListeners.find(
        (httpListener, index) => index !== currentIndex && httpListener.path === contextPath && httpListener.host === fullHost,
      ) != null
    ) {
      const error = { contextPath: 'Context path is already use.' };
      setTimeout(() => contextPathControl.setErrors(error), 0);
      return { ...inheritErrors, ...error };
    }

    setTimeout(() => subDomainControl.setErrors(null), 0);
    return inheritErrors;
  }

  protected getValue(): PathV4[] {
    return this.listenerFormArray?.controls.map((control) => {
      return {
        path: control.get('path').value,
        host: combineSubDomainWithDomain(control.get('_hostSubDomain').value, control.get('_hostDomain').value),
      };
    });
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
