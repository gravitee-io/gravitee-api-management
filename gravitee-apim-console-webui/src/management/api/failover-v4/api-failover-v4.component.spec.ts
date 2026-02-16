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
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

import { ApiFailoverV4Component } from './api-failover-v4.component';
import { ApiFailoverV4Module } from './api-failover-v4.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiV4FailoverComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiFailoverV4Component>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiFailoverV4Module],
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
    fixture = TestBed.createComponent(ApiFailoverV4Component);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should enable and set failover config', async () => {
    const api = fakeApiV4({
      id: API_ID,
      failover: undefined,
    });
    expectApiGetRequest(api);
    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(false);

    // Check each field is disabled
    const maxRetriesInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxRetries"]' }));
    expect(await maxRetriesInput.isDisabled()).toBe(true);
    expect(await maxRetriesInput.getValue()).toEqual('2');

    const slowCallDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="slowCallDuration"]' }));
    expect(await slowCallDurationInput.isDisabled()).toBe(true);
    expect(await slowCallDurationInput.getValue()).toEqual('2000');

    const openStateDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="openStateDuration"]' }));
    expect(await openStateDurationInput.isDisabled()).toBe(true);
    expect(await openStateDurationInput.getValue()).toEqual('10000');

    const maxFailuresInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxFailures"]' }));
    expect(await maxFailuresInput.isDisabled()).toBe(true);
    expect(await maxFailuresInput.getValue()).toEqual('5');

    const perSubscriptionToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="perSubscription"]' }));
    expect(await perSubscriptionToggle.isChecked()).toEqual(true);
    expect(await perSubscriptionToggle.isDisabled()).toBe(true);

    // Enable Failover & set some values
    await enabledSlideToggle.toggle();

    await maxRetriesInput.setValue('2');
    await slowCallDurationInput.setValue('200');
    await openStateDurationInput.setValue('2000');
    await maxFailuresInput.setValue('2');

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.failover).toStrictEqual({
      enabled: true,
      maxRetries: 2,
      slowCallDuration: 200,
      openStateDuration: 2000,
      maxFailures: 2,
      perSubscription: true,
    });
  });

  it.each(['kafka', 'mock'])('should update failover config for %p endpoint', async endpointType => {
    const api = fakeApiV4({
      id: API_ID,
      endpointGroups: [
        {
          name: 'default-group',
          type: endpointType,
          loadBalancer: {
            type: 'ROUND_ROBIN',
          },
          endpoints: [
            {
              name: 'default',
              type: endpointType,
              weight: 1,
              inheritConfiguration: false,
              configuration: {},
            },
          ],
        },
      ],
      failover: {
        enabled: true,
        maxRetries: 2,
        slowCallDuration: 200,
        openStateDuration: 2000,
        maxFailures: 2,
        perSubscription: true,
      },
    });
    expectApiGetRequest(api);
    const saveBar = await loader.getHarness(GioSaveBarHarness);

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(true);

    // Check each field is disabled
    const maxRetriesInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxRetries"]' }));
    expect(await maxRetriesInput.isDisabled()).toBe(false);
    expect(await maxRetriesInput.getValue()).toEqual('2');
    await maxRetriesInput.setValue('3');

    const slowCallDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="slowCallDuration"]' }));
    expect(await slowCallDurationInput.isDisabled()).toBe(false);
    expect(await slowCallDurationInput.getValue()).toEqual('200');
    await slowCallDurationInput.setValue('300');

    const openStateDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="openStateDuration"]' }));
    expect(await openStateDurationInput.isDisabled()).toBe(false);
    expect(await openStateDurationInput.getValue()).toEqual('2000');
    await openStateDurationInput.setValue('3000');

    const maxFailuresInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxFailures"]' }));
    expect(await maxFailuresInput.isDisabled()).toBe(false);
    expect(await maxFailuresInput.getValue()).toEqual('2');
    await maxFailuresInput.setValue('3');

    const perSubscriptionToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="perSubscription"]' }));
    await perSubscriptionToggle.toggle();
    expect(await perSubscriptionToggle.isChecked()).toEqual(false);

    // Verify warning is displayed
    const warningBanner = await loader.getAllHarnesses(DivHarness.with({ selector: '.banner__wrapper__title' }));
    if (endpointType === 'kafka') {
      expect(await warningBanner[0].getText()).toContain(
        'Failover is not supported for Kafka endpoints. Enabling it will have no effect. Use the native Kafka Failover by providing multiple bootstrap servers.',
      );
      expect(await warningBanner[4].getText()).toContain('The circuit breaker will be configured for the whole API');
    } else {
      expect(await warningBanner[3].getText()).toContain('The circuit breaker will be configured for the whole API');
    }

    expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
    await saveBar.clickSubmit();

    // Confirm saving with global circuit breaker
    const dialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
    await dialog.confirm();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.failover).toStrictEqual({
      enabled: true,
      maxRetries: 3,
      slowCallDuration: 300,
      openStateDuration: 3000,
      maxFailures: 3,
      perSubscription: false,
    });
  });

  it('should disable fields when origin is kubernetes', async () => {
    const api = fakeApiV4({
      id: API_ID,
      failover: undefined,
      definitionContext: { origin: 'KUBERNETES' },
    });
    expectApiGetRequest(api);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(false);

    // Check each field is disabled
    const maxRetriesInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxRetries"]' }));
    expect(await maxRetriesInput.isDisabled()).toBe(true);

    const slowCallDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="slowCallDuration"]' }));
    expect(await slowCallDurationInput.isDisabled()).toBe(true);

    const openStateDurationInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="openStateDuration"]' }));
    expect(await openStateDurationInput.isDisabled()).toBe(true);

    const maxFailuresInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="maxFailures"]' }));
    expect(await maxFailuresInput.isDisabled()).toBe(true);

    const perSubscriptionToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="perSubscription"]' }));
    expect(await perSubscriptionToggle.isDisabled()).toBe(true);
    expect(await perSubscriptionToggle.isChecked()).toEqual(true);
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
