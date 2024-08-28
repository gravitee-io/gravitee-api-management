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
import { MatInputHarness } from '@angular/material/input/testing';

import { LogInComponent } from './log-in.component';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('LogInComponent', () => {
  let fixture: ComponentFixture<LogInComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogInComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LogInComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should allow submit if username and password are valid', async () => {
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');

    expect(await submitButton.isDisabled()).toEqual(true);

    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(false);
  });

  it('should not validate form with missing username', async () => {
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(true);
  });
  it('should not validate form with missing password', async () => {
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');

    expect(await submitButton.isDisabled()).toEqual(true);
  });

  it('should login and fetch current user on submit', async () => {
    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));
    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');
    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(false);
    await submitButton.click();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`).flush({});
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user`).flush({});
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-menu-links`).flush({});
  });
});
