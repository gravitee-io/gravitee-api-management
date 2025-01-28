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
import { MatCardHarness } from '@angular/material/card/testing';
import { MatErrorHarness } from '@angular/material/form-field/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { ActivatedRoute } from '@angular/router';

import { ResetPasswordComponent } from './reset-password.component';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    console.error = jest.fn();
    const mockActivatedRoute = {
      snapshot: {
        params: {
          id: '123',
        },
      },
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent, AppTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: mockActivatedRoute }],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should initialize the form with an empty username', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    expect(await usernameInput.getValue()).toEqual('');
  });

  it('should mark the form as invalid if username is empty', async () => {
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));
    expect(await submitButton.isDisabled()).toBe(true);
  });

  it('should mark the form as valid if username is provided', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await usernameInput.setValue('testuser');
    expect(await submitButton.isDisabled()).toBe(false);
  });

  it('should call resetPasswordService.resetPassword on form submission', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await usernameInput.setValue('testuser');
    await submitButton.click();

    expectResetPassword({ username: 'testuser', reset_page_url: 'http://localhost//confirm' });
    expect(fixture.componentInstance.isSubmitted).toBe(true);
  });

  it('should handle errors from resetPasswordService.resetPassword', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await usernameInput.setValue('testuser');
    await submitButton.click();
    expectResetPasswordError({ username: 'testuser', reset_page_url: 'http://localhost//confirm' });

    const error = await harnessLoader.getHarness(MatErrorHarness);
    expect(await error.getText()).toEqual('Password reset failed. Please try again or contact your administrator for assistance.');
    expect(fixture.componentInstance.isSubmitted).toBe(false);
  });

  it('should update the UI after successful form submission', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await usernameInput.setValue('testuser');
    await submitButton.click();

    expectResetPassword({ username: 'testuser', reset_page_url: 'http://localhost//confirm' });
    fixture.detectChanges();

    const card = await harnessLoader.getHarness(MatCardHarness);
    const cardTitle = await card.getTitleText();

    expect(cardTitle).toContain('Password reset confirmed');
  });

  const expectResetPassword = (expectedParams: { username: string; reset_page_url: string }): void => {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_reset_password`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(expectedParams);
    req.flush({});
  };
  const expectResetPasswordError = (expectedParams: { username: string; reset_page_url: string }): void => {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/users/_reset_password`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(expectedParams);
    req.error(new ProgressEvent('error'), { status: 500 });
  };
});
