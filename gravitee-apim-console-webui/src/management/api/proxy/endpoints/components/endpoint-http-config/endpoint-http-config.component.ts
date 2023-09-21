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

import { EndpointGroupV2, HttpHeader, ProtocolVersion } from '../../../../../../entities/management-api-v2';

export interface EndpointHttpConfigValue {
  httpClientOptions: {
    version: ProtocolVersion;
    connectTimeout: number;
    readTimeout: number;
    idleTimeout: number;
    maxConcurrentConnections: number;
    keepAlive: boolean;
    pipelining: boolean;
    useCompression: boolean;
    followRedirects: boolean;
    propagateClientAcceptEncoding?: boolean;
    clearTextUpgrade?: boolean;
  };
  headers: HttpHeader[];
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

    return new FormGroup({
      httpClientOptions,
      headers: new FormControl(endpointGroup.headers ?? []),
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
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
