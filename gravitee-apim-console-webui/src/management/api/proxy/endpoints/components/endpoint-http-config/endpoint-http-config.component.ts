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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';
import { filter, startWith, takeUntil } from 'rxjs/operators';

import {
  EndpointGroupV2,
  HttpClientOptions,
  HttpClientSslOptions,
  HttpHeader,
  HttpProxy,
  HttpProxyType,
  ProtocolVersion,
} from '../../../../../../entities/management-api-v2';

export interface EndpointHttpConfigValue {
  httpClientOptions: HttpClientOptions;
  headers: HttpHeader[];
  httpProxy: HttpProxy;
  httpClientSslOptions?: HttpClientSslOptions;
}

@Component({
  selector: 'endpoint-http-config',
  template: require('./endpoint-http-config.component.html'),
  styles: [require('./endpoint-http-config.component.scss')],
})
export class EndpointHttpConfigComponent implements OnInit, OnDestroy {
  public static getHttpConfigFormGroup(endpointGroup: EndpointGroupV2, isReadonly: boolean): FormGroup {
    const httpClientOptions = new FormGroup({
      version: new FormControl({
        value: endpointGroup.httpClientOptions?.version,
        disabled: isReadonly,
      }),
      connectTimeout: new FormControl({
        value: endpointGroup.httpClientOptions?.connectTimeout,
        disabled: isReadonly,
      }),
      readTimeout: new FormControl({
        value: endpointGroup.httpClientOptions?.readTimeout,
        disabled: isReadonly,
      }),
      idleTimeout: new FormControl({
        value: endpointGroup.httpClientOptions?.idleTimeout,
        disabled: isReadonly,
      }),
      maxConcurrentConnections: new FormControl({
        value: endpointGroup.httpClientOptions?.maxConcurrentConnections,
        disabled: isReadonly,
      }),
      keepAlive: new FormControl({
        value: endpointGroup.httpClientOptions?.keepAlive,
        disabled: isReadonly,
      }),
      pipelining: new FormControl({
        value: endpointGroup.httpClientOptions?.pipelining,
        disabled: isReadonly,
      }),
      useCompression: new FormControl({
        value: endpointGroup.httpClientOptions?.useCompression,
        disabled: isReadonly,
      }),
      followRedirects: new FormControl({
        value: endpointGroup.httpClientOptions?.followRedirects,
        disabled: isReadonly,
      }),
      propagateClientAcceptEncoding: new FormControl({
        value: endpointGroup.httpClientOptions?.propagateClientAcceptEncoding,
        disabled: isReadonly,
      }),
      clearTextUpgrade: new FormControl({
        value: endpointGroup.httpClientOptions?.clearTextUpgrade,
        disabled: isReadonly,
      }),
    });

    const httpProxy = new FormGroup({
      enabled: new FormControl({
        value: endpointGroup.httpProxy?.enabled,
        disabled: isReadonly,
      }),
      useSystemProxy: new FormControl({
        value: endpointGroup.httpProxy?.useSystemProxy,
        disabled: isReadonly,
      }),
      host: new FormControl({
        value: endpointGroup.httpProxy?.host,
        disabled: isReadonly,
      }),
      port: new FormControl({
        value: endpointGroup.httpProxy?.port,
        disabled: isReadonly,
      }),
      type: new FormControl(
        {
          value: endpointGroup.httpProxy?.type ?? 'HTTP',
          disabled: isReadonly,
        },
        Validators.required,
      ),
      username: new FormControl({
        value: endpointGroup.httpProxy?.username,
        disabled: isReadonly,
      }),
      password: new FormControl({
        value: endpointGroup.httpProxy?.password,
        disabled: isReadonly,
      }),
    });

    const httpClientSslOptions = new FormGroup({
      hostnameVerifier: new FormControl({
        value: endpointGroup.httpClientSslOptions?.hostnameVerifier,
        disabled: isReadonly,
      }),
      trustAll: new FormControl({
        value: endpointGroup.httpClientSslOptions?.trustAll,
        disabled: isReadonly,
      }),
      trustStore: new FormControl({
        value: endpointGroup.httpClientSslOptions?.trustStore,
        disabled: isReadonly,
      }),
      keyStore: new FormControl({
        value: endpointGroup.httpClientSslOptions?.keyStore,
        disabled: isReadonly,
      }),
    });

    return new FormGroup({
      httpClientOptions,
      headers: new FormControl({
        value: endpointGroup.headers ?? [],
        disabled: isReadonly,
      }),
      httpProxy,
      httpClientSslOptions,
    });
  }

  public static getHttpConfigValue(httpConfigFormGroup: FormGroup): EndpointHttpConfigValue {
    return httpConfigFormGroup.getRawValue();
  }

  @Input()
  public httpConfigFormGroup: FormGroup;

  public httpVersions: { label: string; value: ProtocolVersion }[] = [
    {
      label: 'HTTP/1.1',
      value: 'HTTP_1_1',
    },
    {
      label: 'HTTP/2',
      value: 'HTTP_2',
    },
  ];

  public proxyTypes: { label: string; value: HttpProxyType }[] = [
    {
      label: 'HTTP CONNECT proxy',
      value: 'HTTP',
    },
    {
      label: 'SOCKS4/4a tcp proxy',
      value: 'SOCKS4',
    },
    {
      label: 'SOCKS5 tcp proxy',
      value: 'SOCKS5',
    },
  ];

  private unsubscribe$ = new Subject<boolean>();

  ngOnInit(): void {
    if (!this.httpConfigFormGroup) {
      throw new Error('httpConfigFormGroup input is required');
    }

    const httpClientOptions = this.httpConfigFormGroup.get('httpClientOptions');

    httpClientOptions
      .get('version')
      .valueChanges.pipe(
        startWith(httpClientOptions.get('version').value),
        // Only if version is not disabled
        filter(() => !httpClientOptions.get('version').disabled),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((value) => {
        const isHttp2 = value === 'HTTP_2';
        if (isHttp2) {
          httpClientOptions.get('clearTextUpgrade').enable();
        } else {
          if (httpClientOptions.get('clearTextUpgrade').value === true) {
            httpClientOptions.get('clearTextUpgrade').setValue(false);
          }
          httpClientOptions.get('clearTextUpgrade').disable();
        }
      });

    httpClientOptions
      .get('useCompression')
      .valueChanges.pipe(
        startWith(httpClientOptions.get('useCompression').value),
        // Only if useCompression is not disabled
        filter(() => !httpClientOptions.get('useCompression').disabled),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((value) => {
        const useCompression = !!value;
        if (useCompression) {
          if (httpClientOptions.get('propagateClientAcceptEncoding').value === true) {
            httpClientOptions.get('propagateClientAcceptEncoding').setValue(false);
          }
          httpClientOptions.get('propagateClientAcceptEncoding').disable();
        } else {
          httpClientOptions.get('propagateClientAcceptEncoding').enable();
        }
      });

    const httpProxy = this.httpConfigFormGroup.get('httpProxy') as FormGroup;

    httpProxy
      .get('enabled')
      .valueChanges.pipe(
        startWith(httpProxy.get('enabled').value),
        // Only if enabled is not disabled
        filter(() => !httpProxy.get('enabled').disabled),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((enabled) => {
        if (enabled === true) {
          httpProxy.get('useSystemProxy').enable();
          httpProxy.get('host').enable();
          httpProxy.get('host').addValidators(Validators.required);
          httpProxy.get('port').enable();
          httpProxy.get('port').addValidators(Validators.required);
          httpProxy.get('type').enable();
          httpProxy.get('username').enable();
          httpProxy.get('password').enable();
        } else {
          httpProxy.get('useSystemProxy').disable();
          httpProxy.get('useSystemProxy').setValue(false);
          httpProxy.get('host').disable();
          httpProxy.get('host').clearValidators();
          httpProxy.get('port').disable();
          httpProxy.get('port').clearValidators();
          httpProxy.get('type').disable();
          httpProxy.get('username').disable();
          httpProxy.get('password').disable();
        }

        // Update validators
        Object.keys(httpProxy.controls).forEach((controlName) => {
          httpProxy.get(controlName)?.updateValueAndValidity({ emitEvent: false });
        });
        httpProxy.updateValueAndValidity();
      });

    httpProxy
      .get('useSystemProxy')
      .valueChanges.pipe(
        startWith(httpProxy.get('useSystemProxy').value),
        // Only if useSystemProxy is not disabled
        filter(() => !httpProxy.get('useSystemProxy').disabled),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((useSystemProxy) => {
        if (useSystemProxy === true) {
          httpProxy.get('host').disable();
          httpProxy.get('host').addValidators(Validators.required);
          httpProxy.get('port').disable();
          httpProxy.get('port').addValidators(Validators.required);
          httpProxy.get('type').disable();
          httpProxy.get('username').disable();
          httpProxy.get('password').disable();
        } else {
          httpProxy.get('host').enable();
          httpProxy.get('host').clearValidators();
          httpProxy.get('port').enable();
          httpProxy.get('port').clearValidators();
          httpProxy.get('type').enable();
          httpProxy.get('username').enable();
          httpProxy.get('password').enable();
        }

        // Update validators
        Object.keys(httpProxy.controls).forEach((controlName) => {
          httpProxy.get(controlName)?.updateValueAndValidity({ emitEvent: false });
        });
        httpProxy.updateValueAndValidity();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
