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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { of, throwError } from 'rxjs';

import { ResetPasswordComponent } from './reset-password.component';
import { ResetPasswordService } from '../../services/reset-password.service';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let harnessLoader: HarnessLoader;
  let mockResetPasswordService: Partial<ResetPasswordService>;
  let originalConsoleError: any;

  beforeEach(async () => {
    mockResetPasswordService = {
      resetPassword: () => of({}),
    };

    originalConsoleError = console.error;
    console.error = jest.fn();

    const mockActivatedRoute = {
      snapshot: {
        params: {
          id: '123',
        },
      },
    };

    await TestBed.configureTestingModule({
      imports: [
        ResetPasswordComponent,
        ReactiveFormsModule,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: ResetPasswordService, useValue: mockResetPasswordService },
        { provide: ActivatedRoute, useValue: { mockActivatedRoute } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    console.error = originalConsoleError;
  });

  it('should create the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
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

    let resetPasswordCalled = false;
    mockResetPasswordService.resetPassword = () => {
      resetPasswordCalled = true;
      return of({});
    };

    await usernameInput.setValue('testuser');
    await submitButton.click();

    expect(resetPasswordCalled).toBe(true);
    expect(fixture.componentInstance.isSubmitted).toBe(true);
  });

  it('should handle errors from resetPasswordService.resetPassword', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    mockResetPasswordService.resetPassword = () => throwError(() => new Error('Test Error'));

    await usernameInput.setValue('testuser');
    await submitButton.click();

    expect(fixture.componentInstance.isSubmitted).toBe(false);

    expect(console.error).toHaveBeenCalledWith('Reset password error:', expect.any(Error));
  });

  it('should update the UI after successful form submission', async () => {
    const usernameInput = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: /Reset password/i }));

    await usernameInput.setValue('testuser');
    await submitButton.click();

    fixture.detectChanges();

    const cardTitle = fixture.nativeElement.querySelector('mat-card-title');
    expect(cardTitle.textContent).toContain('Password reset confirmed');
  });
});
