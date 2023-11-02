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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { EndpointHttpConfigComponent } from './endpoint-http-config.component';
import { EndpointHttpConfigModule } from './endpoint-http-config.module';
import { EndpointHttpConfigHarness } from './endpoint-http-config.harness';

import { GioHttpTestingModule } from '../../../../../../shared/testing';
import { EndpointGroupV2 } from '../../../../../../entities/management-api-v2';

describe('ApiPropertiesComponent', () => {
  let fixture: ComponentFixture<EndpointHttpConfigComponent>;
  let component: EndpointHttpConfigComponent;
  let httpTestingController: HttpTestingController;
  let endpointHttpConfigHarness: EndpointHttpConfigHarness;

  let initialEndpointGroupV2: EndpointGroupV2;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, EndpointHttpConfigModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(EndpointHttpConfigComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;

    initialEndpointGroupV2 = {
      httpClientOptions: {
        version: 'HTTP_2',
        connectTimeout: 1000,
        readTimeout: 1000,
        idleTimeout: 1000,
        maxConcurrentConnections: 1000,
        keepAlive: true,
        pipelining: true,
        useCompression: true,
        followRedirects: true,
        propagateClientAcceptEncoding: true,
        clearTextUpgrade: true,
      },
      httpProxy: {
        enabled: true,
        useSystemProxy: false,
        type: 'HTTP',
        host: 'host',
        port: 1000,
        username: 'username',
        password: 'password',
      },
    };
    component.httpConfigFormGroup = EndpointHttpConfigComponent.getHttpConfigFormGroup(initialEndpointGroupV2, false);
    fixture.detectChanges();
    endpointHttpConfigHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EndpointHttpConfigHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init form fields with value', async () => {
    const values = await endpointHttpConfigHarness.getHttpConfigValues();

    expect(values).toEqual({
      httpClientOptions: {
        clearTextUpgrade: true,
        connectTimeout: 1000,
        followRedirects: true,
        idleTimeout: 1000,
        keepAlive: true,
        maxConcurrentConnections: 1000,
        pipelining: true,
        propagateClientAcceptEncoding: false,
        readTimeout: 1000,
        useCompression: true,
        version: 'HTTP_2',
      },
      headers: [],
      httpProxy: initialEndpointGroupV2.httpProxy,
    });
  });

  it('should change HttpVersion to HTTP 1.1', async () => {
    const clearTextUpgrade = await endpointHttpConfigHarness.getMatSlideToggle('clearTextUpgrade');

    expect(await clearTextUpgrade.isDisabled()).toEqual(false);

    await endpointHttpConfigHarness.setHttpVersion('HTTP/1.1');

    expect(await clearTextUpgrade.isDisabled()).toEqual(true);

    expect(await endpointHttpConfigHarness.getHttpConfigValues()).toEqual({
      httpClientOptions: {
        clearTextUpgrade: false,
        connectTimeout: 1000,
        followRedirects: true,
        idleTimeout: 1000,
        keepAlive: true,
        maxConcurrentConnections: 1000,
        pipelining: true,
        propagateClientAcceptEncoding: false,
        readTimeout: 1000,
        useCompression: true,
        version: 'HTTP_1_1',
      },
      headers: [],
      httpProxy: initialEndpointGroupV2.httpProxy,
    });
  });

  it('should set useCompression=false and propagateClientAcceptEncoding=true', async () => {
    const propagateClientAcceptEncoding = await endpointHttpConfigHarness.getMatSlideToggle('propagateClientAcceptEncoding');
    expect(await propagateClientAcceptEncoding.isDisabled()).toEqual(true);

    await endpointHttpConfigHarness.setEnableCompression(false);
    expect(await propagateClientAcceptEncoding.isDisabled()).toEqual(false);

    await propagateClientAcceptEncoding.toggle();

    expect(await endpointHttpConfigHarness.getHttpConfigValues()).toEqual({
      httpClientOptions: {
        clearTextUpgrade: true,
        connectTimeout: 1000,
        followRedirects: true,
        idleTimeout: 1000,
        keepAlive: true,
        maxConcurrentConnections: 1000,
        pipelining: true,
        propagateClientAcceptEncoding: true,
        readTimeout: 1000,
        useCompression: false,
        version: 'HTTP_2',
      },
      headers: [],
      httpProxy: initialEndpointGroupV2.httpProxy,
    });
  });

  it('should set httpProxy with system proxy', async () => {
    await endpointHttpConfigHarness.setHttpProxy({
      enabled: true,
      useSystemProxy: true,
    });

    const httpProxy = await endpointHttpConfigHarness.getHttpProxyValues();

    expect(httpProxy).toEqual({
      enabled: true,
      useSystemProxy: true,
      host: 'host',
      password: 'password',
      port: 1000,
      type: 'HTTP',
      username: 'username',
    });

    const httpProxyGroup = component.httpConfigFormGroup.get('httpProxy');
    expect(httpProxyGroup.get('host').disabled).toEqual(true);
    expect(httpProxyGroup.get('port').disabled).toEqual(true);
    expect(httpProxyGroup.get('type').disabled).toEqual(true);
    expect(httpProxyGroup.get('username').disabled).toEqual(true);
    expect(httpProxyGroup.get('password').disabled).toEqual(true);
  });

  it('should set httpProxy ', async () => {
    await endpointHttpConfigHarness.setHttpProxy({
      enabled: true,
      useSystemProxy: false,
      host: '',
      port: undefined,
    });

    // host & port are required
    expect(component.httpConfigFormGroup.get('httpProxy.host').invalid).toEqual(true);
    expect(component.httpConfigFormGroup.get('httpProxy.port').invalid).toEqual(true);
    expect(component.httpConfigFormGroup.invalid).toEqual(true);

    await endpointHttpConfigHarness.setHttpProxy({
      enabled: true,
      useSystemProxy: false,
      host: 'new host',
      port: 42,
      username: 'foo',
      password: 'bar',
      type: 'HTTP',
    });
    expect(component.httpConfigFormGroup.invalid).toEqual(false);

    const httpProxy = await endpointHttpConfigHarness.getHttpProxyValues();

    expect(httpProxy).toEqual({
      enabled: true,
      useSystemProxy: false,
      type: 'HTTP',
      host: 'new host',
      port: 42,
      username: 'foo',
      password: 'bar',
    });
  });

  it('should add headers', async () => {
    const httpFormHeaders = await endpointHttpConfigHarness.getHttpFormHeaders();

    await httpFormHeaders.addHeader({ key: 'key1', value: 'value1' });

    expect(await endpointHttpConfigHarness.getHttpConfigValues()).toEqual({
      httpClientOptions: {
        clearTextUpgrade: true,
        connectTimeout: 1000,
        followRedirects: true,
        idleTimeout: 1000,
        keepAlive: true,
        maxConcurrentConnections: 1000,
        pipelining: true,
        propagateClientAcceptEncoding: false,
        readTimeout: 1000,
        useCompression: true,
        version: 'HTTP_2',
      },
      headers: [
        {
          name: 'key1',
          value: 'value1',
        },
      ],
      httpProxy: initialEndpointGroupV2.httpProxy,
    });
  });

  it('should add SSL Options', async () => {
    const hostnameVerifier = await endpointHttpConfigHarness.getVerifyHostname();
    await hostnameVerifier.toggle();

    const trustAll = await endpointHttpConfigHarness.getTrustAll();
    await trustAll.toggle();

    const sslKeyStoreForm = await endpointHttpConfigHarness.getTrustStore();
    expect(sslKeyStoreForm).toEqual(null);

    const keyStore = await endpointHttpConfigHarness.getKeyStore();
    await keyStore.setJksKeyStore({
      type: 'JKS',
      password: 'password',
      path: 'path',
    });

    expect(EndpointHttpConfigComponent.getHttpConfigValue(component.httpConfigFormGroup).httpClientSslOptions).toEqual({
      hostnameVerifier: true,
      keyStore: {
        content: null,
        password: 'password',
        path: 'path',
        type: 'JKS',
      },
      trustAll: true,
      trustStore: undefined,
    });
  });
});
