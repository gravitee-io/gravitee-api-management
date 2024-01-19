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
  AsyncValidatorFn,
  FormArray,
  FormControl,
  FormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
} from '@angular/forms';
import { escapeRegExp, isEmpty } from 'lodash';
import { Observable, of, zip } from 'rxjs';
import { map } from 'rxjs/operators';

import { GioFormListenersContextPathComponent } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.component';
import { PathV4 } from '../../../../../entities/management-api-v2';

interface InternalPathV4 extends PathV4 {
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

  public newListenerFormGroup(listener: PathV4): FormGroup {
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
    const inheritErrors = this.validateVirtualHostPath(listenerControl);
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

  private validateVirtualHostPath(listenerControl: AbstractControl): ValidationErrors | null {
    const listenerPathControl = listenerControl.get('path');
    const contextPath: string = listenerPathControl.value;

    const errors = this.validateGenericPathListenerControl(contextPath);

    setTimeout(() => listenerPathControl.setErrors(errors), 0);
    return errors;
  }

  protected listenersAsyncValidator(): AsyncValidatorFn {
    return (listenerFormArrayControl: FormArray): Observable<ValidationErrors | null> => {
      const listenerFormArrayControls = listenerFormArrayControl.controls;

      const contextPathsToIgnore = this.pathsToIgnore?.map((p) => p.path) ?? [];
      const pathValidations$: Observable<ValidationErrors | null>[] = listenerFormArrayControls.map((listenerControl) => {
        const host = combineSubDomainWithDomain(listenerControl.get('_hostSubDomain').value, listenerControl.get('_hostDomain').value);

        const listenerPathControl = listenerControl.get('path');
        const contextPathValue = listenerPathControl.value;
        if (contextPathsToIgnore.includes(contextPathValue)) {
          return of(null);
        }
        return this.apiService.verify({ host, contextPath: contextPathValue });
      });

      return zip(...pathValidations$).pipe(
        map((errors: (ValidationErrors | null)[]) => {
          errors.forEach((error, index) => {
            setTimeout(() => listenerFormArrayControls.at(index).get('path').setErrors(error), 0);
          });
          if (errors.filter((v) => v !== null).length === 0) {
            return null;
          }
          return { listeners: true };
        }),
      );
    };
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
