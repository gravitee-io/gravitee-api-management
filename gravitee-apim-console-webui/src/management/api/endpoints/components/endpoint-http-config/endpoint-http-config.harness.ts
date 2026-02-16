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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { keyBy, mapValues } from 'lodash';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormHeadersHarness } from '@gravitee/ui-particles-angular';

import { EndpointHttpConfigValue } from './endpoint-http-config.component';

import { SslTrustStoreFormHarness } from '../ssl-truststore-form/ssl-truststore-form.harness';
import { SslKeyStoreFormHarness } from '../ssl-keystore-form/ssl-keystore-form.harness';

const httpClientOptionsControlNames = [
  'version',
  'connectTimeout',
  'readTimeout',
  'keepAliveTimeout',
  'idleTimeout',
  'maxConcurrentConnections',
  'keepAlive',
  'pipelining',
  'useCompression',
  'followRedirects',
  'propagateClientAcceptEncoding',
  'clearTextUpgrade',
];

const httpProxyControlNames = ['enabled', 'useSystemProxy', 'type', 'host', 'port', 'username', 'password'];

export class EndpointHttpConfigHarness extends ComponentHarness {
  static hostSelector = 'endpoint-http-config';

  async getMatSelect(formControlName: string): Promise<MatSelectHarness> {
    return this.locatorForOptional(MatSelectHarness.with({ selector: `[formControlName="${formControlName}"]` }))();
  }

  async getMatSlideToggle(formControlName: string): Promise<MatSlideToggleHarness> {
    return this.locatorForOptional(MatSlideToggleHarness.with({ selector: `[formControlName="${formControlName}"]` }))();
  }

  async getMatInput(formControlName: string): Promise<MatInputHarness> {
    return this.locatorForOptional(MatInputHarness.with({ selector: `[formControlName="${formControlName}"]` }))();
  }

  async getHttpFormHeaders(): Promise<GioFormHeadersHarness> {
    return this.locatorForOptional(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }))();
  }

  async getVerifyHostname(): Promise<MatSlideToggleHarness> {
    return this.locatorForOptional(MatSlideToggleHarness.with({ selector: '[formControlName="hostnameVerifier"]' }))();
  }

  async getTrustAll(): Promise<MatSlideToggleHarness> {
    return this.locatorForOptional(MatSlideToggleHarness.with({ selector: '[formControlName="trustAll"]' }))();
  }

  async getTrustStore(): Promise<SslTrustStoreFormHarness> {
    return this.locatorForOptional(SslTrustStoreFormHarness)();
  }

  async getKeyStore(): Promise<SslKeyStoreFormHarness> {
    return this.locatorForOptional(SslKeyStoreFormHarness)();
  }

  async getHttpConfigValues(): Promise<EndpointHttpConfigValue> {
    const httpFormHeaders = await this.getHttpFormHeaders().then(h => h?.getHeaderRows());

    const headersNameValue = (
      await parallel(() => httpFormHeaders.map(async h => ({ name: await h.keyInput.getValue(), value: await h.valueInput.getValue() })))
    ).filter(
      h => h.name, // remove last empty row
    );

    return {
      httpClientOptions: await this.getHttpClientOptions(),
      httpProxy: await this.getHttpProxyValues(),
      headers: headersNameValue,
    };
  }

  async setHttpVersion(versionLabel: string): Promise<void> {
    const matSelect = await this.getMatSelect('version');

    if (await matSelect.isDisabled()) {
      throw new Error('Http version is disabled');
    }

    await matSelect.clickOptions({ text: versionLabel });
  }

  async setEnableCompression(enable: boolean): Promise<void> {
    const matSlideToggle = await this.getMatSlideToggle('useCompression');

    if (await matSlideToggle.isDisabled()) {
      throw new Error('Use compression is disabled');
    }

    if ((await matSlideToggle.isChecked()) !== enable) {
      await matSlideToggle.toggle();
    }
  }

  async getHttpClientOptions(): Promise<EndpointHttpConfigValue['httpClientOptions']> {
    const httpClientOptionsKeyValues = await parallel(() =>
      httpClientOptionsControlNames.map(async formControlName => {
        let value = null;
        if (formControlName === 'version') {
          const labelToValue = {
            'HTTP/1.1': 'HTTP_1_1',
            'HTTP/2': 'HTTP_2',
          };

          value = await this.getMatSelect(formControlName)
            .then(matSelect => matSelect?.getValueText())
            .then(v => labelToValue[v]);
        }
        if (
          ['keepAlive', 'pipelining', 'useCompression', 'followRedirects', 'propagateClientAcceptEncoding', 'clearTextUpgrade'].includes(
            formControlName,
          )
        ) {
          value = await this.getMatSlideToggle(formControlName).then(matSlideToggle => matSlideToggle?.isChecked());
        }
        if (['connectTimeout', 'readTimeout', 'keepAliveTimeout', 'idleTimeout', 'maxConcurrentConnections'].includes(formControlName)) {
          value = await this.getMatInput(formControlName)
            .then(matInput => matInput?.getValue())
            .then(v => (Number(v) ? Number(v) : v));
        }

        return {
          key: formControlName,
          value,
        };
      }),
    );

    return mapValues(keyBy(httpClientOptionsKeyValues, 'key'), 'value') as EndpointHttpConfigValue['httpClientOptions'];
  }

  async getHttpProxyValues(): Promise<EndpointHttpConfigValue['httpProxy']> {
    const httpProxyKeyValues = await parallel(() =>
      httpProxyControlNames.map(async formControlName => {
        let value = null;
        if (formControlName === 'type') {
          const labelToValue = {
            'HTTP CONNECT proxy': 'HTTP',
            'SOCKS4/4a tcp proxy': 'SOCKS4',
            'SOCKS5 tcp proxy': 'SOCKS5',
          };

          value = await this.getMatSelect(formControlName).then(matSelect => matSelect?.getValueText().then(v => labelToValue[v]));
        }
        if (['enabled', 'useSystemProxy'].includes(formControlName)) {
          value = await this.getMatSlideToggle(formControlName).then(matSlideToggle => matSlideToggle?.isChecked());
        }
        if (['host', 'port', 'username', 'password'].includes(formControlName)) {
          value = await this.getMatInput(formControlName)
            .then(matInput => matInput?.getValue())
            .then(v => (Number(v) ? Number(v) : v));
        }

        return {
          key: formControlName,
          value,
        };
      }),
    );

    return mapValues(keyBy(httpProxyKeyValues, 'key'), 'value') as EndpointHttpConfigValue['httpProxy'];
  }

  async setHttpProxy(value: EndpointHttpConfigValue['httpProxy']): Promise<void> {
    const enable = await this.locatorFor(MatSlideToggleHarness.with({ name: 'httpProxyEnabled' }))();

    if (value.enabled) {
      await enable.check();

      if (Object.prototype.hasOwnProperty.call(value, 'useSystemProxy')) {
        const useSystemProxy = await this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="useSystemProxy"]' }))();
        value.useSystemProxy ? await useSystemProxy.check() : await useSystemProxy.uncheck();
      }

      if (Object.prototype.hasOwnProperty.call(value, 'host')) {
        const host = await this.locatorFor(MatInputHarness.with({ selector: '[formControlName="host"]' }))();
        await host.setValue(value.host);
      }

      if (Object.prototype.hasOwnProperty.call(value, 'type')) {
        const type = await this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="type"]' }))();
        await type.clickOptions({ text: new RegExp(value.type, 'i') });
      }

      if (Object.prototype.hasOwnProperty.call(value, 'port')) {
        const port = await this.locatorFor(MatInputHarness.with({ selector: '[formControlName="port"]' }))();
        await port.setValue(`${value.port}`);
      }

      if (Object.prototype.hasOwnProperty.call(value, 'username')) {
        const username = await this.locatorFor(MatInputHarness.with({ selector: '[formControlName="username"]' }))();
        await username.setValue(value.username);
      }

      if (Object.prototype.hasOwnProperty.call(value, 'password')) {
        const password = await this.locatorFor(MatInputHarness.with({ selector: '[formControlName="password"]' }))();
        await password.setValue(value.password);
      }
    }
  }
}
