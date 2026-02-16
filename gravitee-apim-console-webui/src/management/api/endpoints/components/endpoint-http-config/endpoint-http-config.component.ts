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

import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { asyncScheduler, merge, Subject } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { filter, startWith, takeUntil, map, observeOn } from 'rxjs/operators';

import {
  EndpointGroupV2,
  EndpointV2,
  HttpClientOptions,
  HttpClientSslOptions,
  HttpHeader,
  HttpProxy,
  HttpProxyType,
  ProtocolVersion,
} from '../../../../../entities/management-api-v2';

export interface EndpointHttpConfigValue {
  httpClientOptions: HttpClientOptions;
  headers: HttpHeader[];
  httpProxy: HttpProxy;
  httpClientSslOptions?: HttpClientSslOptions;
}

@Component({
  selector: 'endpoint-http-config',
  templateUrl: './endpoint-http-config.component.html',
  styleUrls: ['./endpoint-http-config.component.scss'],
  standalone: false,
})
export class EndpointHttpConfigComponent implements OnDestroy, OnChanges {
  public static getHttpConfigFormGroup(endpointGroup: EndpointGroupV2 | EndpointV2, isReadonly: boolean): UntypedFormGroup {
    const httpClientOptions = new UntypedFormGroup({
      version: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.version ?? 'HTTP_1_1',
        disabled: isReadonly,
      }),
      connectTimeout: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.connectTimeout ?? 5000,
        disabled: isReadonly,
      }),
      readTimeout: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.readTimeout ?? 10000,
        disabled: isReadonly,
      }),
      keepAliveTimeout: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.keepAliveTimeout ?? 30000,
        disabled: isReadonly,
      }),
      idleTimeout: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.idleTimeout ?? 60000,
        disabled: isReadonly,
      }),
      maxConcurrentConnections: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.maxConcurrentConnections ?? 100,
        disabled: isReadonly,
      }),
      keepAlive: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.keepAlive ?? true,
        disabled: isReadonly,
      }),
      pipelining: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.pipelining,
        disabled: isReadonly,
      }),
      useCompression: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.useCompression ?? true,
        disabled: isReadonly,
      }),
      followRedirects: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.followRedirects,
        disabled: isReadonly,
      }),
      propagateClientAcceptEncoding: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.propagateClientAcceptEncoding,
        disabled: isReadonly,
      }),
      clearTextUpgrade: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.clearTextUpgrade,
        disabled: isReadonly,
      }),
      maxHeaderSize: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.maxHeaderSize ?? 8192,
        disabled: isReadonly,
      }),
      maxChunkSize: new UntypedFormControl({
        value: endpointGroup.httpClientOptions?.maxChunkSize ?? 8192,
        disabled: isReadonly,
      }),
    });

    const httpProxy = new UntypedFormGroup({
      enabled: new UntypedFormControl({
        value: endpointGroup.httpProxy?.enabled ?? false,
        disabled: isReadonly,
      }),
      useSystemProxy: new UntypedFormControl({
        value: endpointGroup.httpProxy?.useSystemProxy,
        disabled: isReadonly,
      }),
      host: new UntypedFormControl(
        {
          value: endpointGroup.httpProxy?.host,
          disabled: isReadonly,
        },
        Validators.required,
      ),
      port: new UntypedFormControl(
        {
          value: endpointGroup.httpProxy?.port,
          disabled: isReadonly,
        },
        Validators.required,
      ),
      type: new UntypedFormControl({ value: endpointGroup.httpProxy?.type ?? 'HTTP', disabled: isReadonly }, Validators.required),
      username: new UntypedFormControl({
        value: endpointGroup.httpProxy?.username,
        disabled: isReadonly,
      }),
      password: new UntypedFormControl({
        value: endpointGroup.httpProxy?.password,
        disabled: isReadonly,
      }),
    });

    const httpClientSslOptions = new UntypedFormGroup({
      hostnameVerifier: new UntypedFormControl({
        value: endpointGroup.httpClientSslOptions?.hostnameVerifier,
        disabled: isReadonly,
      }),
      trustAll: new UntypedFormControl({
        value: endpointGroup.httpClientSslOptions?.trustAll,
        disabled: isReadonly,
      }),
      trustStore: new UntypedFormControl({
        value: endpointGroup.httpClientSslOptions?.trustStore,
        disabled: isReadonly,
      }),
      keyStore: new UntypedFormControl({
        value: endpointGroup.httpClientSslOptions?.keyStore,
        disabled: isReadonly,
      }),
    });

    return new UntypedFormGroup({
      httpClientOptions,
      headers: new UntypedFormControl({
        value: endpointGroup.headers ?? [],
        disabled: isReadonly,
      }),
      httpProxy,
      httpClientSslOptions,
    });
  }

  public static getHttpConfigValue(httpConfigFormGroup: UntypedFormGroup): EndpointHttpConfigValue {
    return httpConfigFormGroup.getRawValue();
  }

  @Input()
  public httpConfigFormGroup: UntypedFormGroup;

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

  constructor(private readonly changeDetectorRef: ChangeDetectorRef) {}

  ngOnChanges(): void {
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
      .subscribe(value => {
        const isHttp2 = value === 'HTTP_2';
        if (isHttp2) {
          httpClientOptions.get('clearTextUpgrade').enable({ emitEvent: false });
        } else {
          if (httpClientOptions.get('clearTextUpgrade').value === true) {
            httpClientOptions.get('clearTextUpgrade').setValue(false, { emitEvent: false });
          }
          httpClientOptions.get('clearTextUpgrade').disable({ emitEvent: false });
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
      .subscribe(value => {
        const useCompression = !!value;
        if (useCompression) {
          if (httpClientOptions.get('propagateClientAcceptEncoding').value === true) {
            httpClientOptions.get('propagateClientAcceptEncoding').setValue(false, { emitEvent: false });
          }
          httpClientOptions.get('propagateClientAcceptEncoding').disable({ emitEvent: false });
        } else {
          httpClientOptions.get('propagateClientAcceptEncoding').enable({ emitEvent: false });
        }
      });

    const httpProxy = this.httpConfigFormGroup.get('httpProxy') as UntypedFormGroup;

    merge(httpProxy.get('enabled').valueChanges, httpProxy.get('useSystemProxy').valueChanges)
      .pipe(
        startWith({}),
        // Only if enabled is not disabled
        filter(() => !httpProxy.get('enabled').disabled),
        map(() => [httpProxy.get('enabled').value, httpProxy.get('useSystemProxy').value]),
        observeOn(asyncScheduler),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(([enabled, useSystemProxy]) => {
        if (enabled === true && useSystemProxy !== true) {
          httpProxy.get('useSystemProxy').enable({ emitEvent: false });
          httpProxy.get('host').enable({ onlySelf: true }); // onlySelf to refresh UI `*`
          httpProxy.get('port').enable({ onlySelf: true });
          httpProxy.get('type').enable({ emitEvent: false });
          httpProxy.get('username').enable({ emitEvent: false });
          httpProxy.get('password').enable({ emitEvent: false });
        } else if (enabled === true && useSystemProxy === true) {
          httpProxy.get('useSystemProxy').enable({ emitEvent: false });
          httpProxy.get('host').disable({ onlySelf: true });
          httpProxy.get('port').disable({ onlySelf: true });
          httpProxy.get('type').disable({ emitEvent: false });
          httpProxy.get('username').disable({ emitEvent: false });
          httpProxy.get('password').disable({ emitEvent: false });
        } else {
          httpProxy.get('useSystemProxy').disable({ emitEvent: false });
          httpProxy.get('useSystemProxy').setValue(false, { emitEvent: false });
          httpProxy.get('host').disable({ onlySelf: true });
          httpProxy.get('port').disable({ onlySelf: true });
          httpProxy.get('type').disable({ emitEvent: false });
          httpProxy.get('username').disable({ emitEvent: false });
          httpProxy.get('password').disable({ emitEvent: false });
        }

        // Update validators
        Object.keys(httpProxy.controls).forEach(controlName => {
          httpProxy.get(controlName)?.updateValueAndValidity({ emitEvent: false });
        });
        httpProxy.updateValueAndValidity();
        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
