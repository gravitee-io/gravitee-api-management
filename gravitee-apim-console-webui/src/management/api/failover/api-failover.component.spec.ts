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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';

import { ApiFailoverComponent } from './api-failover.component';
import { ApiFailoverModule } from './api-failover.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { ApiV2, fakeApiV2 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProxyFailoverComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiFailoverComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiFailoverModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u'] },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiFailoverComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should enable and set failover config', async () => {
    const api = fakeApiV2({
      id: API_ID,
      proxy: {
        failover: undefined,
      },
    });
    expectApiGetRequest(api);
    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(false);

    // Check each field is disabled
    const maxAttemptsInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxAttempts"]' }));
    expect(await maxAttemptsInput.isDisabled()).toBe(true);

    const retryTimeoutInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="retryTimeout"]' }));
    expect(await retryTimeoutInput.isDisabled()).toBe(true);

    // Enable Failover & set some values
    await enabledSlideToggle.toggle();

    await maxAttemptsInput.setValue('2');
    await retryTimeoutInput.setValue('22');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.proxy.failover).toStrictEqual({
      cases: ['TIMEOUT'],
      maxAttempts: 2,
      retryTimeout: 22,
    });
  });

  it('should update failover config', async () => {
    const api = fakeApiV2({
      id: API_ID,
      proxy: {
        failover: {
          maxAttempts: 2,
          retryTimeout: 22,
        },
      },
    });
    expectApiGetRequest(api);
    const saveBar = await loader.getHarness(GioSaveBarHarness);

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(true);

    const maxAttemptsInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxAttempts"]' }));
    expect(await maxAttemptsInput.getValue()).toEqual('2');
    await maxAttemptsInput.setValue('3');

    const retryTimeoutInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="retryTimeout"]' }));
    expect(await retryTimeoutInput.getValue()).toEqual('22');
    await retryTimeoutInput.setValue('33');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.proxy.failover).toStrictEqual({
      cases: ['TIMEOUT'],
      maxAttempts: 3,
      retryTimeout: 33,
    });
  });

  it('should disable fields when origin is kubernetes', async () => {
    const api = fakeApiV2({
      id: API_ID,
      proxy: {
        failover: undefined,
      },
      definitionContext: { origin: 'KUBERNETES' },
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const maxAttemptsInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxAttempts"]' }));
    expect(await maxAttemptsInput.isDisabled()).toBe(true);

    const retryTimeoutInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="retryTimeout"]' }));
    expect(await retryTimeoutInput.isDisabled()).toBe(true);
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
