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
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { GioFormCronHarness, GioFormHeadersHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';

import { ApiDynamicPropertiesComponent } from './api-dynamic-properties.component';
import { ApiDynamicPropertiesModule } from './api-dynamic-properties.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../../shared/testing/gio-uirouter-testing-module';
import { Api, fakeApiV2 } from '../../../../../entities/management-api-v2';

describe('ApiDynamicPropertiesComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiDynamicPropertiesComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiDynamicPropertiesModule, GioUiRouterTestingModule, MatIconTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: API_ID } }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDynamicPropertiesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display configured dynamic properties', async () => {
    expectGetApi(
      fakeApiV2({
        id: API_ID,
        services: {
          dynamicProperty: {
            enabled: true,
            provider: 'HTTP',
            schedule: '0 */42 * * * *',
            configuration: {
              method: 'GET',
              url: 'http://localhost:8083',
              specification: '[{operation=default, spec={}}]',
              body: 'body',
              headers: [{ value: 'headerValue', name: 'headerName' }],
              useSystemProxy: false,
            },
          },
        },
      }),
    );

    const enabledInput = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledInput.isChecked()).toBe(true);

    const scheduleInput = await loader.getHarness(GioFormCronHarness.with({ selector: '[formControlName="schedule"]' }));
    expect(await scheduleInput.getValue()).toBe('0 */42 * * * *');

    const methodInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    expect(await methodInput.getValueText()).toBe('GET');

    const urlInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="url"]' }));
    expect(await urlInput.getValue()).toBe('http://localhost:8083');

    const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
    expect(await (await headersInput.getHeaderRows())[0].keyInput.getValue()).toBe('headerName');
    expect(await (await headersInput.getHeaderRows())[0].valueInput.getValue()).toBe('headerValue');

    const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
    expect(await bodyInput.getValue()).toBe('body');

    const specificationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="specification"]' }));
    expect(await specificationInput.getValue()).toBe('[{operation=default, spec={}}]');
  });

  it('should disable/enable dynamic properties', async () => {
    expectGetApi(
      fakeApiV2({
        id: API_ID,
        services: {
          dynamicProperty: undefined,
        },
      }),
    );

    const enabledInput = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledInput.isChecked()).toBe(false);

    const scheduleInput = await loader.getHarness(GioFormCronHarness.with({ selector: '[formControlName="schedule"]' }));
    expect(await scheduleInput.isDisabled()).toBe(true);

    const methodInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    expect(await methodInput.isDisabled()).toBe(true);

    const urlInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="url"]' }));
    expect(await urlInput.isDisabled()).toBe(true);

    const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
    expect(await headersInput.isDisabled()).toBe(true);

    const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
    expect(await bodyInput.isDisabled()).toBe(true);

    const specificationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="specification"]' }));
    expect(await specificationInput.isDisabled()).toBe(true);

    await enabledInput.toggle();
    expect(await enabledInput.isChecked()).toBe(true);

    expect(await scheduleInput.isDisabled()).toBe(false);
    expect(await methodInput.isDisabled()).toBe(false);
    expect(await urlInput.isDisabled()).toBe(false);
    expect(await headersInput.isDisabled()).toBe(false);
    expect(await bodyInput.isDisabled()).toBe(false);
    expect(await specificationInput.isDisabled()).toBe(false);
  });

  it('should set dynamic properties', async () => {
    expectGetApi(
      fakeApiV2({
        id: API_ID,
        services: {
          dynamicProperty: undefined,
        },
      }),
    );

    const enabledInput = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    await enabledInput.toggle();

    const scheduleInput = await loader.getHarness(GioFormCronHarness.with({ selector: '[formControlName="schedule"]' }));
    await scheduleInput.setCustomValue('0 */42 * * * *');

    const methodInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    await methodInput.clickOptions({ text: 'POST' });

    const urlInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="url"]' }));
    await urlInput.setValue('http://localhost:8083');

    const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
    await bodyInput.setValue('body');

    const specificationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="specification"]' }));
    await specificationInput.setValue('specification');

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectGetApi(
      fakeApiV2({
        id: API_ID,
        services: {
          dynamicProperty: undefined,
        },
      }),
    );
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });
    expect(req.request.body.services.dynamicProperty).toEqual({
      enabled: true,
      provider: 'HTTP',
      schedule: '0 */42 * * * *',

      configuration: {
        method: 'POST',
        url: 'http://localhost:8083',
        body: 'body',
        specification: 'specification',
      },
    });
  });

  function expectGetApi(api: Api) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
    });
    req.flush(api);
  }
});
