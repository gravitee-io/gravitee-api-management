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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ApiKeyValidationHarness } from './api-key-validation.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiSubscriptionsModule } from '../../api-subscriptions.module';
import { VerifySubscription } from '../../../../../entities/management-api-v2';

const API_ID = 'my-api-id';
const APP_ID = 'my-app-id';

@Component({
  selector: 'test-component',
  template: `<api-key-validation [formControl]="apiKey" [apiId]="apiId" [applicationId]="applicationId"></api-key-validation>`,
  standalone: false,
})
class TestComponent {
  apiId: string = API_ID;
  applicationId: string = APP_ID;
  apiKey: FormControl = new FormControl('');
}
describe('ApiKeyValidationComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [ApiSubscriptionsModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ReactiveFormsModule],
      declarations: [TestComponent],
      providers: [
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
            isTabbable: () => true, // Allows tabbing and avoids warnings
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await init();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should be invalid if less than 8 characters', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    await harness.setInputValue('1234567');

    expect(await harness.isValid()).toEqual(false);
    expect(fixture.componentInstance.apiKey.touched).toEqual(true);
  });
  it('should be invalid if more than 64 characters', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    await harness.setInputValue(
      'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
    );

    expect(await harness.isValid()).toEqual(false);
    expect(fixture.componentInstance.apiKey.touched).toEqual(true);
  });
  it('should be invalid if contains special characters', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    await harness.setInputValue('12?34567');

    expect(await harness.isRequired()).toEqual(false);
    expect(await harness.isValid()).toEqual(false);
    expect(fixture.componentInstance.apiKey.touched).toEqual(true);
  });
  it('should be invalid if API Key not unique', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    expect(fixture.componentInstance.apiKey.touched).toEqual(false);

    await harness.setInputValue('123456789');

    const httpMatches = httpTestingController.match({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_verify`,
      method: 'POST',
    });
    expect(httpMatches.length).toEqual(2);
    expect(httpMatches[0].cancelled).toEqual(true);
    const verifySubscription: VerifySubscription = {
      applicationId: APP_ID,
      apiKey: '123456789',
    };
    expect(httpMatches[1].request.body).toEqual(verifySubscription);
    httpMatches[1].flush({ ok: false });

    expect(await harness.isRequired()).toEqual(false);
    expect(await harness.isValid()).toEqual(false);
    expect(fixture.componentInstance.apiKey.touched).toEqual(true);
  });
  it('should be valid if optional but empty', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    expect(await harness.getInputValue()).toEqual('');
    expect(await harness.isRequired()).toEqual(false);
    expect(await harness.isValid()).toEqual(true);
    expect(fixture.componentInstance.apiKey.touched).toEqual(false);
  });
  it('should be valid with a valid input', async () => {
    const harness = await loader.getHarness(ApiKeyValidationHarness);
    await harness.setInputValue('12345678');

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_verify`,
      method: 'POST',
    });
    const verifySubscription: VerifySubscription = {
      applicationId: APP_ID,
      apiKey: '12345678',
    };
    expect(req.request.body).toEqual(verifySubscription);
    req.flush({ ok: true });

    expect(await harness.isValid()).toEqual(true);
    expect(fixture.componentInstance.apiKey.touched).toEqual(true);
  });
});
