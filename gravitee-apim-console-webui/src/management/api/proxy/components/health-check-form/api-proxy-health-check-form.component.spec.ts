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
import { GioFormHeadersHarness } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiProxyHealthCheckFormComponent } from './api-proxy-health-check-form.component';
import { ApiProxyHealthCheckFormModule } from './api-proxy-health-check-form.module';

import { CurrentUserService } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { GioHttpTestingModule } from '../../../../../shared/testing';

describe('ApiProxyHealthCheckFormComponent', () => {
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  let fixture: ComponentFixture<ApiProxyHealthCheckFormComponent>;
  let loader: HarnessLoader;
  let component: ApiProxyHealthCheckFormComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyHealthCheckFormModule, MatIconTestingModule],
      providers: [{ provide: CurrentUserService, useValue: { currentUser } }],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyHealthCheckFormComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    component = fixture.componentInstance;
    fixture.detectChanges();

    component.healthCheckForm = ApiProxyHealthCheckFormComponent.NewHealthCheckFormGroup();
    component.ngOnChanges({ healthCheckForm: {} } as any);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be disabled by default', async () => {
    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(false);

    // Trigger
    expect(component.healthCheckForm.get('schedule').disabled).toEqual(true);

    // Request
    const allowMethodsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    expect(await allowMethodsInput.isDisabled()).toEqual(true);

    const pathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="path"]' }));
    expect(await pathInput.isDisabled()).toEqual(true);

    const fromRootSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="fromRoot"]' }));
    expect(await fromRootSlideToggle.isDisabled()).toEqual(true);

    // Not body, method is not selected
    expect(await (await loader.getAllHarnesses(MatInputHarness.with({ selector: '[formControlName="body"]' }))).length).toEqual(0);

    const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
    expect(await headersInput.isDisabled()).toEqual(true);

    // Assertion
    const addAssertionButton = await loader.getHarness(MatButtonHarness.with({ text: /Add assertion/ }));
    expect(await addAssertionButton.isDisabled()).toEqual(true);

    const assertion = await loader.getHarness(MatInputHarness.with({ selector: '[ng-reflect-name="0"]' }));
    expect(await assertion.isDisabled()).toEqual(true);

    expect(component.healthCheckForm.value).toEqual({
      enabled: false,
    });
  });

  it('should add health check', async () => {
    // Enable health check
    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(false);
    await enabledSlideToggle.check();

    // Trigger
    component.healthCheckForm.get('schedule').setValue('* * * * *');
    expect(component.healthCheckForm.get('schedule').disabled).toEqual(false);

    // Request
    const allowMethodsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    expect(await allowMethodsInput.isDisabled()).toEqual(false);
    await allowMethodsInput.clickOptions({ text: 'POST' });

    const pathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="path"]' }));
    expect(await pathInput.isDisabled()).toEqual(false);
    await pathInput.setValue('/test');

    const fromRootSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="fromRoot"]' }));
    expect(await fromRootSlideToggle.isDisabled()).toEqual(false);
    await fromRootSlideToggle.check();

    const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
    expect(await bodyInput.isDisabled()).toEqual(false);
    await bodyInput.setValue('The body');

    const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
    expect(await headersInput.isDisabled()).toEqual(false);
    await headersInput.addHeader({ key: 'X-Test', value: 'test' });

    // Assertion
    // add assertion button
    const addAssertionButton = await loader.getHarness(MatButtonHarness.with({ text: /Add assertion/ }));
    expect(await addAssertionButton.isDisabled()).toEqual(false);
    await addAssertionButton.click();

    const assertion_1 = await loader.getHarness(MatInputHarness.with({ selector: '[ng-reflect-name="1"]' }));
    await assertion_1.setValue('new assertion');

    expect(ApiProxyHealthCheckFormComponent.HealthCheckFromFormGroup(component.healthCheckForm)).toEqual({
      enabled: true,
      schedule: '* * * * *',
      steps: [
        {
          request: {
            method: 'POST',
            path: '/test',
            body: 'The body',
            headers: [{ name: 'X-Test', value: 'test' }],
            fromRoot: true,
          },
          response: {
            assertions: ['#response.status == 200', 'new assertion'],
          },
        },
      ],
    });
  });

  it('should display configured Health Check', async () => {
    const healthCheck: ApiProxyHealthCheckFormModule = {
      enabled: true,
      schedule: '* * * * *',
      steps: [
        {
          request: {
            method: 'PUT',
            path: '/test',
            body: 'The body',
            headers: [{ name: 'X-Test', value: 'test' }],
            fromRoot: true,
          },
          response: {
            assertions: ['#response.status == 400'],
          },
        },
      ],
    };

    component.healthCheckForm = ApiProxyHealthCheckFormComponent.NewHealthCheckFormGroup(healthCheck);
    fixture.detectChanges();
    component.ngOnChanges({ healthCheckForm: {} } as any);
    fixture.detectChanges();

    const enabledSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    expect(await enabledSlideToggle.isChecked()).toEqual(true);

    // Trigger
    expect(component.healthCheckForm.get('schedule').disabled).toEqual(false);
    expect(component.healthCheckForm.get('schedule').value).toEqual('* * * * *');

    // Request
    const allowMethodsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="method"]' }));
    expect(await allowMethodsInput.isDisabled()).toEqual(false);
    expect(await allowMethodsInput.getValueText()).toEqual('PUT');

    const pathInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="path"]' }));
    expect(await pathInput.isDisabled()).toEqual(false);
    expect(await pathInput.getValue()).toEqual('/test');

    const fromRootSlideToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="fromRoot"]' }));
    expect(await fromRootSlideToggle.isDisabled()).toEqual(false);
    expect(await fromRootSlideToggle.isChecked()).toEqual(true);

    const bodyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="body"]' }));
    expect(await bodyInput.isDisabled()).toEqual(false);
    expect(await bodyInput.getValue()).toEqual('The body');

    const headersInput = await loader.getHarness(GioFormHeadersHarness.with({ selector: '[formControlName="headers"]' }));
    expect(await headersInput.isDisabled()).toEqual(false);
    const headerRow1 = (await headersInput.getHeaderRows())[0];
    expect(await headerRow1.keyInput.getValue()).toEqual('X-Test');
    expect(await headerRow1.valueInput.getValue()).toEqual('test');

    // Assertion
    const addAssertionButton = await loader.getHarness(MatButtonHarness.with({ text: /Add assertion/ }));
    expect(await addAssertionButton.isDisabled()).toEqual(false);

    const assertion = await loader.getHarness(MatInputHarness.with({ selector: '[ng-reflect-name="0"]' }));
    expect(await assertion.isDisabled()).toEqual(false);
    expect(await assertion.getValue()).toEqual('#response.status == 400');

    expect(ApiProxyHealthCheckFormComponent.HealthCheckFromFormGroup(component.healthCheckForm)).toEqual(healthCheck);
  });
});
