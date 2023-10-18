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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiProxyHealthCheckComponent } from './api-proxy-health-check.component';
import { ApiProxyHealthCheckModule } from './api-proxy-health-check.module';

import { User } from '../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2, fakeApiV2 } from '../../../../entities/management-api-v2';

describe('ApiProxyHealthCheckComponent', () => {
  const currentUser = new User();
  currentUser.userPermissions = ['api-health-c'];
  const fakeUiRouter = { go: jest.fn() };
  const API_ID = 'my-api';

  let fixture: ComponentFixture<ApiProxyHealthCheckComponent>;
  let loader: HarnessLoader;
  let component: ApiProxyHealthCheckComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyHealthCheckModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterState, useValue: fakeUiRouter },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyHealthCheckComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should add health check', async () => {
    const api = fakeApiV2({
      id: API_ID,
      services: {
        healthCheck: {
          enabled: false,
        },
      },
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);

    // Enable health check
    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    await enabledSlideToggle.check();

    // Trigger
    component.healthCheckForm.get('schedule').setValue('* * * * *');
    expect(component.healthCheckForm.get('schedule').disabled).toEqual(false);

    // Request
    const allowMethodsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    await allowMethodsInput.clickOptions({ text: 'POST' });

    const pathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="path"]' }));
    await pathInput.setValue('/test');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);

    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.services['healthCheck']).toStrictEqual({
      enabled: true,
      schedule: '* * * * *',
      steps: [
        {
          request: {
            method: 'POST',
            path: '/test',
            headers: [],
            body: undefined,
            fromRoot: undefined,
          },
          response: {
            assertions: ['#response.status == 200'],
          },
        },
      ],
    });
  });

  it('should activate health check for all endpoints with no health check config', async () => {
    const api = fakeApiV2({
      id: API_ID,
      services: {
        healthCheck: {
          enabled: false,
        },
      },
      proxy: {
        groups: [
          {
            name: 'default',
            endpoints: [
              { name: 'endpoint1-with-healthcheck-deactivated', healthCheck: { enabled: false, inherit: false }, type: 'http' },
              { name: 'endpoint1-with-healthcheck-activated', healthCheck: { enabled: true, inherit: true }, type: 'http' },
              { name: 'endpoint1-without-healthcheck', type: 'http' },
            ],
          },
          {
            name: 'group-2',
            endpoints: [
              { name: 'endpoint2-with-healthcheck-deactivated', healthCheck: { enabled: false, inherit: false }, type: 'http' },
              { name: 'endpoint2-with-healthcheck-activated', healthCheck: { enabled: true, inherit: true }, type: 'http' },
              { name: 'endpoint2-without-healthcheck', type: 'http' },
            ],
          },
        ],
      },
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);

    // Enable health check
    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    await enabledSlideToggle.check();

    // Trigger
    component.healthCheckForm.get('schedule').setValue('* * * * *');
    expect(component.healthCheckForm.get('schedule').disabled).toEqual(false);

    // Request
    const allowMethodsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    await allowMethodsInput.clickOptions({ text: 'POST' });

    const pathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="path"]' }));
    await pathInput.setValue('/test');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });
    expect(req.request.body.proxy.groups).toStrictEqual([
      {
        name: 'default',
        endpoints: [
          { name: 'endpoint1-with-healthcheck-deactivated', healthCheck: { enabled: false, inherit: false }, type: 'http' },
          { name: 'endpoint1-with-healthcheck-activated', healthCheck: { inherit: true }, type: 'http' },
          { name: 'endpoint1-without-healthcheck', healthCheck: { inherit: true }, type: 'http' },
        ],
      },
      {
        name: 'group-2',
        endpoints: [
          { name: 'endpoint2-with-healthcheck-deactivated', healthCheck: { enabled: false, inherit: false }, type: 'http' },
          { name: 'endpoint2-with-healthcheck-activated', healthCheck: { inherit: true }, type: 'http' },
          { name: 'endpoint2-without-healthcheck', healthCheck: { inherit: true }, type: 'http' },
        ],
      },
    ]);
  });

  it('should deactivate health check for all endpoints with "inherit" health check config', async () => {
    const api = fakeApiV2({
      id: API_ID,
      services: {
        healthCheck: {
          enabled: true,
          schedule: '* * * * *',
          steps: [
            {
              request: {
                method: 'POST',
                path: '/test',
                headers: [],
                body: undefined,
                fromRoot: undefined,
              },
              response: {
                assertions: ['#response.status == 200'],
              },
            },
          ],
        },
      },
      proxy: {
        groups: [
          {
            name: 'default',
            endpoints: [
              { name: 'endpoint1-with-healthcheck-deactivated', healthCheck: { enabled: false }, type: 'http' },
              { name: 'endpoint1-with-healthcheck-activated-inherited', healthCheck: { enabled: true, inherit: true }, type: 'http' },
              { name: 'endpoint1-with-healthcheck-activated', healthCheck: { enabled: true, inherit: false }, type: 'http' },
              { name: 'endpoint1-without-healthcheck', type: 'http' },
            ],
          },
          {
            name: 'group-2',
            endpoints: [
              { name: 'endpoint2-with-healthcheck-deactivated', healthCheck: { enabled: false }, type: 'http' },
              { name: 'endpoint2-with-healthcheck-activated-inherited', healthCheck: { enabled: true, inherit: true }, type: 'http' },
              { name: 'endpoint2-with-healthcheck-activated', healthCheck: { enabled: true, inherit: false }, type: 'http' },
              { name: 'endpoint2-without-healthcheck', type: 'http' },
            ],
          },
        ],
      },
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);

    // Disable health check
    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    await enabledSlideToggle.uncheck();

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });
    expect(req.request.body.services['healthCheck']).toStrictEqual({
      enabled: false,
    });

    expect(req.request.body.proxy.groups).toStrictEqual([
      {
        name: 'default',
        endpoints: [
          { name: 'endpoint1-with-healthcheck-deactivated', healthCheck: { inherit: false, enabled: false }, type: 'http' },
          { name: 'endpoint1-with-healthcheck-activated-inherited', healthCheck: { inherit: true }, type: 'http' },
          { name: 'endpoint1-with-healthcheck-activated', healthCheck: { enabled: true, inherit: false }, type: 'http' },
          { name: 'endpoint1-without-healthcheck', healthCheck: { inherit: true }, type: 'http' },
        ],
      },
      {
        name: 'group-2',
        endpoints: [
          { name: 'endpoint2-with-healthcheck-deactivated', healthCheck: { inherit: false, enabled: false }, type: 'http' },
          { name: 'endpoint2-with-healthcheck-activated-inherited', healthCheck: { inherit: true }, type: 'http' },
          { name: 'endpoint2-with-healthcheck-activated', healthCheck: { enabled: true, inherit: false }, type: 'http' },
          { name: 'endpoint2-without-healthcheck', healthCheck: { inherit: true }, type: 'http' },
        ],
      },
    ]);
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
