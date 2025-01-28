/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { ActivatedRoute } from '@angular/router';

import { ResetPasswordConfirmationComponent } from './reset-password-confirmation.component';
import { TokenService } from '../../../../services/token.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ResetPasswordConfirmationComponent', () => {
  let fixture: ComponentFixture<ResetPasswordConfirmationComponent>;
  let component: ResetPasswordConfirmationComponent;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let mockTokenService: Partial<TokenService>;

  beforeEach(async () => {
    mockTokenService = {
      parseToken: () => ({
        firstname: 'John',
        lastname: 'Doe',
        email: 'john.doe@example.com',
      }),
      isParsedTokenExpired: () => false,
    };

    const mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: () => 'mock-token',
          has: () => true,
          getAll: () => ['mock-token'],
          keys: ['token'],
        },
      },
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordConfirmationComponent, AppTestingModule],
      providers: [
        { provide: TokenService, useValue: mockTokenService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordConfirmationComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should mark the form as invalid if passwords do not match', async () => {
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmedPasswordInput = await harnessLoader.getHarness(
      MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }),
    );
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await passwordInput.setValue('password123');
    await confirmedPasswordInput.setValue('password456');

    expect(await submitButton.isDisabled()).toBeTruthy();
  });

  it('should mark the form as valid if passwords match', async () => {
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmedPasswordInput = await harnessLoader.getHarness(
      MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }),
    );
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await passwordInput.setValue('password123');
    await confirmedPasswordInput.setValue('password123');

    expect(await submitButton.isDisabled()).toBeFalsy();
  });

  it('should call resetPasswordService.confirmResetPassword on form submission', async () => {
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    const confirmedPasswordInput = await harnessLoader.getHarness(
      MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }),
    );
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await passwordInput.setValue('password123');
    await confirmedPasswordInput.setValue('password123');
    await submitButton.click();

    expectResetPassword();
  });

  it('should handle errors from resetPasswordService.confirmResetPassword', async () => {
    const passwordInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await passwordInput.setValue('password123');

    const confirmedPasswordInput = await harnessLoader.getHarness(
      MatInputHarness.with({ selector: '[formControlName="confirmedPassword"]' }),
    );
    await confirmedPasswordInput.setValue('password123');

    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));
    await submitButton.click();
    expectResetPasswordError();

    const error = await harnessLoader.getHarness(MatErrorHarness);
    expect(await error.getText()).toEqual('Password reset failed. Please try again or contact your administrator for assistance.');
    expect(component.isSubmitted).toBeFalsy();
  });

  const expectResetPassword = (): void => {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_change_password`);
    expect(req.request.method).toEqual('POST');
    req.flush({});
  };
  const expectResetPasswordError = (): void => {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_change_password`);
    expect(req.request.method).toEqual('POST');
    req.error(new ProgressEvent('error'), { status: 500 });
  };
});
