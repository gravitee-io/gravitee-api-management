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
  AsyncValidatorFn,
  NG_ASYNC_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { escapeRegExp, isEmpty } from 'lodash';
import { Observable, of, timer } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { GioFormListenersContextPathComponent } from '../gio-form-listeners-context-path/gio-form-listeners-context-path.component';
import { PathV4 } from '../../../../../entities/management-api-v2';
import { virtualHostModePathPathSyncValidator } from '../../../../../shared/validators/context-path/context-path-sync-validator.directive';

interface InternalPathV4 extends PathV4 {
  _hostSubDomain?: string;
  _hostDomain?: string;
}

@Component({
  selector: 'gio-form-listeners-virtual-host',
  templateUrl: './gio-form-listeners-virtual-host.component.html',
  styleUrls: ['../gio-form-listeners.common.scss', './gio-form-listeners-virtual-host.component.scss'],
  standalone: false,
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

  public override newListenerFormGroup(listener: PathV4): UntypedFormGroup {
    const { host, hostDomain } = extractDomainToHost(listener?.host, this.domainRestrictions);

    return new UntypedFormGroup(
      {
        host: new UntypedFormControl(listener?.host || ''),
        // Private controls for internal process
        _hostSubDomain: new UntypedFormControl(host || ''),
        _hostDomain: new UntypedFormControl(hostDomain || ''),
        path: new UntypedFormControl(listener.path, { validators: [virtualHostModePathPathSyncValidator] }),
        overrideAccess: new UntypedFormControl(listener.overrideAccess || false),
      },
      { validators: [this.validateListenerControl()], asyncValidators: [this.listenersAsyncValidator()] },
    );
  }

  // From ControlValueAccessor interface
  public override registerOnChange(fn: (listeners: PathV4[] | null) => void): void {
    this._onChange = (listeners: InternalPathV4[]) =>
      fn(
        listeners.map((listener) => ({
          ...listener,
          host: combineSubDomainWithDomain(listener._hostSubDomain, listener._hostDomain),
        })),
      );
  }

  validateListenerControl(): ValidatorFn {
    return (formGroup: UntypedFormGroup): ValidationErrors | null => {
      if (formGroup.pristine) {
        return null;
      }

      const subDomainControl = formGroup.get('_hostSubDomain');
      const domainControl = formGroup.get('_hostDomain');
      const fullHost = combineSubDomainWithDomain(subDomainControl.value, domainControl.value);

      // When no domain restrictions, host is required
      if (isEmpty(this.domainRestrictions) && isEmpty(subDomainControl.value)) {
        return { host: 'Host is required.' };
      }

      if (!isEmpty(this.domainRestrictions) && !this.domainRestrictions.some((domainRestriction) => fullHost.endsWith(domainRestriction))) {
        return { host: 'Host is not valid (must end with one of restriction domain).' };
      }
      return null;
    };
  }

  public override listenersValidator(): ValidatorFn {
    return (formArray: UntypedFormArray): ValidationErrors | null => {
      const listenerFormArrayControls = formArray.controls;
      const listenerValues = listenerFormArrayControls.map((listener) => ({
        path: listener.value?.path && listener.value?.path?.endsWith('/') ? listener.value?.path : listener.value?.path + '/',
        _hostSubDomain: listener.value?._hostSubDomain,
        _hostDomain: listener.value?._hostDomain,
        host: listener.value?.host,
      }));
      if (new Set(listenerValues.map((value) => JSON.stringify(value))).size !== listenerValues.length) {
        return { contextPath: 'Duplicated virtual host not allowed' };
      }
      return null;
    };
  }

  protected override getValue(): PathV4[] {
    return this.listenerFormArray?.controls.map((control) => {
      return {
        path: control.get('path').value,
        host: combineSubDomainWithDomain(control.get('_hostSubDomain').value, control.get('_hostDomain').value),
      };
    });
  }

  public listenersAsyncValidator(): AsyncValidatorFn {
    return (formGroup: UntypedFormGroup): Observable<ValidationErrors | null> => {
      if (formGroup && formGroup.dirty) {
        return timer(250).pipe(
          switchMap(() =>
            this.apiV2Service.verifyPath(this.apiId, [
              {
                path: formGroup.getRawValue().path,
                host: combineSubDomainWithDomain(formGroup.get('_hostSubDomain').value, formGroup.get('_hostDomain').value),
              },
            ]),
          ),
          map((res) => (res.ok ? null : { listeners: res.reason })),
        );
      }
      return of(null);
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
