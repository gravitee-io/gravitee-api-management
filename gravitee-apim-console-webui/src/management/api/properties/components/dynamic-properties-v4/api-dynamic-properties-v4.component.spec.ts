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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiDynamicPropertiesV4Component } from './api-dynamic-properties-v4.component';
import { ApiDynamicPropertiesV4Harness } from './api-dynamic-properties-v4.harness';

import { Api, ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { ApiPropertiesModule } from '../../properties/api-properties.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';

describe('ApiDynamicPropertiesV4Component', () => {
  const API_ID = 'apiId';
  const API_V4 = fakeApiV4({ id: API_ID, properties: [] });
  const SCHEMA = {
    $schema: 'http://json-schema.org/draft-07/schema#',
    type: 'object',
    properties: {
      dummy: {
        title: 'dummy',
        type: 'string',
        description: 'A dummy string',
      },
    },
    required: ['dummy'],
    additionalProperties: false,
  };

  let fixture: ComponentFixture<ApiDynamicPropertiesV4Component>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiDynamicPropertiesV4Harness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPropertiesModule, MatIconTestingModule, RouterTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDynamicPropertiesV4Component);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDynamicPropertiesV4Harness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should navigate to properties page', async () => {
    expectGetApi(API_V4);
    expectDynamicPropertiesConfigurationSchemaGet();

    expect(
      await componentHarness
        .getGoBackButton()
        .then((btn) => btn.host())
        .then((host) => host.getAttribute('ng-reflect-router-link')),
    ).toStrictEqual('../..');
  });

  it('should fill and save the configuration form', async () => {
    expectGetApi(API_V4);
    expectDynamicPropertiesConfigurationSchemaGet();

    expect(await componentHarness.isSaveVisible()).toBeFalsy();
    expect(await componentHarness.getConfigurationInputDisabled('dummy')).toBeTruthy();

    await componentHarness.toggleEnabledFormField();

    expect(await componentHarness.getEnabledFieldToggleValue()).toBeTruthy();
    expect(await componentHarness.isSaveVisible()).toBeTruthy();
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
    expect(await componentHarness.getConfigurationInputDisabled('dummy')).toBeFalsy();
    expect(await componentHarness.getConfigurationInputValue('dummy')).toStrictEqual('');

    await componentHarness.setConfigurationInputValue('dummy', 'configuration value');

    expect(await componentHarness.getConfigurationInputValue('dummy')).toStrictEqual('configuration value');
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.save();

    expectGetApi(API_V4);
    expectApiPutRequest({
      ...API_V4,
      services: {
        dynamicProperty: {
          configuration: {
            dummy: 'configuration value',
          },
          enabled: true,
          type: 'http-dynamic-properties',
        },
      },
    });
    expect(await componentHarness.isSaveVisible()).toBeFalsy();
  });

  it('should disable the dynamic properties', async () => {
    expectGetApi({
      ...API_V4,
      services: {
        dynamicProperty: {
          configuration: {
            dummy: 'configuration value',
          },
          enabled: true,
          type: 'http-dynamic-properties',
        },
      },
    });
    expectDynamicPropertiesConfigurationSchemaGet();
    expect(await componentHarness.isSaveVisible()).toBeFalsy();
    expect(await componentHarness.getEnabledFieldToggleValue()).toBeTruthy();

    await componentHarness.toggleEnabledFormField();

    expect(await componentHarness.getEnabledFieldToggleValue()).toBeFalsy();

    await componentHarness.save();

    expectGetApi(API_V4);
    expectApiPutRequest({
      ...API_V4,
      services: {
        dynamicProperty: {
          configuration: {
            dummy: 'configuration value',
          },
          enabled: false,
          type: 'http-dynamic-properties',
        },
      },
    });
    expect(await componentHarness.getEnabledFieldToggleValue()).toBeFalsy();
  });

  it('should fill and reset the configuration form', async () => {
    expectGetApi(API_V4);
    expectDynamicPropertiesConfigurationSchemaGet();

    expect(await componentHarness.isSaveVisible()).toBeFalsy();
    expect(await componentHarness.getConfigurationInputDisabled('dummy')).toBeTruthy();

    await componentHarness.toggleEnabledFormField();

    expect(await componentHarness.getEnabledFieldToggleValue()).toBeTruthy();
    expect(await componentHarness.isSaveVisible()).toBeTruthy();
    expect(await componentHarness.isSaveDisabled()).toBeTruthy();
    expect(await componentHarness.getConfigurationInputDisabled('dummy')).toBeFalsy();
    expect(await componentHarness.getConfigurationInputValue('dummy')).toStrictEqual('');

    await componentHarness.setConfigurationInputValue('dummy', 'configuration value');

    expect(await componentHarness.getConfigurationInputValue('dummy')).toStrictEqual('configuration value');
    expect(await componentHarness.isSaveDisabled()).toBeFalsy();

    await componentHarness.reset();

    expect(await componentHarness.isSaveVisible()).toBeFalsy();
    expect(await componentHarness.getEnabledFieldToggleValue()).toBeFalsy();
    expect(await componentHarness.getConfigurationInputDisabled('dummy')).toBeTruthy();
    expect(await componentHarness.getConfigurationInputValue('dummy')).toStrictEqual('');
  });

  function expectGetApi(api: Api) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
    });
    req.flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
  }

  function expectDynamicPropertiesConfigurationSchemaGet(): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/api-services/http-dynamic-properties/schema`, method: 'GET' })
      .flush(SCHEMA);
    fixture.detectChanges();
  }
});
