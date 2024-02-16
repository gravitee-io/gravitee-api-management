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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';

import { ResetPasswordComponent } from './reset-password.component';
import { ResetPasswordModule } from './reset-password.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../shared/testing';

describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    httpTestingController.verify();
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ResetPasswordModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                token:
                  'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmOTIwYjAxOC1kZmMxLTQ0MzItYTBiMC0xOGRmYzE2NDMyNjgiLCJmaXJzdG5hbWUiOiJ0ZXN0IiwiaXNzIjoiZ3Jhdml0ZWUtbWFuYWdlbWVudC1hdXRoIiwiYWN0aW9uIjoiVVNFUl9SRUdJU1RSQVRJT04iLCJleHAiOjQyMDEzNTY4OTIsImlhdCI6MTcwMTI3MDQ5MiwiZW1haWwiOiJ0ZXN0QHRlc3QuY29tIiwibGFzdG5hbWUiOiJ0ZXN0In0.tfTLKSuyRaW2KdT6O3wexzkg3tLfo7-Kj6TaPORbtoM',
              },
            },
          },
        },
      ],
    });
    fixture = TestBed.createComponent(ResetPasswordComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should reset password', async () => {
    const firstNameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="firstName"]' }));
    expect(await firstNameInput.isDisabled()).toBe(true);

    const lastNameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="lastName"]' }));
    expect(await lastNameInput.isDisabled()).toBe(true);

    const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
    expect(await emailInput.isDisabled()).toBe(true);

    const passwordInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await passwordInput.setValue('password');

    const confirmPasswordInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="confirmPassword"]' }));
    await confirmPasswordInput.setValue('password');

    const signUpButton = await loader.getHarness(MatButtonHarness.with({ selector: '[type="submit"]' }));
    await signUpButton.click();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.org.baseURL}/users/f920b018-dfc1-4432-a0b0-18dfc1643268/changePassword`,
    });
    expect(req.request.body).toEqual({
      firstname: 'test',
      lastname: 'test',
      token: expect.any(String),
      password: 'password',
    });
  });
});
