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

import { EndpointHttpConfigComponent } from './endpoint-http-config.component';
import { EndpointHttpConfigModule } from './endpoint-http-config.module';
import { EndpointHttpConfigHarness } from './endpoint-http-config.harness';

import { GioHttpTestingModule } from '../../../../../../shared/testing';

describe('ApiPropertiesComponent', () => {
  let fixture: ComponentFixture<EndpointHttpConfigComponent>;
  let component: EndpointHttpConfigComponent;
  let httpTestingController: HttpTestingController;
  let endpointHttpConfigHarness: EndpointHttpConfigHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, EndpointHttpConfigModule],
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

    component.httpConfigFormGroup = EndpointHttpConfigComponent.getHttpConfigFormGroup(
      {
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
      },
      false,
    );
    endpointHttpConfigHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EndpointHttpConfigHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init form fields with value', async () => {
    const values = await endpointHttpConfigHarness.getHttpConfigValues();

    expect(values).toEqual({
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
      version: 'HTTP/2',
    });
  });

  it('should change HttpVersion to HTTP 1.1', async () => {
    const clearTextUpgrade = await endpointHttpConfigHarness.getMatSlideToggle('clearTextUpgrade');

    expect(await clearTextUpgrade.isDisabled()).toEqual(false);

    await endpointHttpConfigHarness.setHttpVersion('HTTP/1.1');

    expect(await clearTextUpgrade.isDisabled()).toEqual(true);

    expect(await endpointHttpConfigHarness.getHttpConfigValues()).toEqual({
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
      version: 'HTTP/1.1',
    });
  });

  it('should set useCompression=false and propagateClientAcceptEncoding=true', async () => {
    const propagateClientAcceptEncoding = await endpointHttpConfigHarness.getMatSlideToggle('propagateClientAcceptEncoding');
    expect(await propagateClientAcceptEncoding.isDisabled()).toEqual(true);

    await endpointHttpConfigHarness.setEnableCompression(false);
    expect(await propagateClientAcceptEncoding.isDisabled()).toEqual(false);

    await propagateClientAcceptEncoding.toggle();

    expect(await endpointHttpConfigHarness.getHttpConfigValues()).toEqual({
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
      version: 'HTTP/2',
    });
  });
});
