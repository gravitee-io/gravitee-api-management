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

import { LoginModule } from './login.module';
import { LoginComponent } from './login.component';

import { CurrentUserService } from '../../ajs-upgraded-providers';
import { User } from '../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../shared/testing';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, LoginModule],
      providers: [{ provide: CurrentUserService, useValue: { currentUser } }],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should login with username & password', async () => {
    const usernameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await usernameInput.setValue('username');

    const passwordInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await passwordInput.setValue('password');

    const loginButton = await loader.getHarness(MatButtonHarness.with({ selector: '[type="submit"]' }));
    await loginButton.click();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.org.baseURL}/user/login`,
    });
    expect(req.request.headers.get('Authorization').startsWith('Basic')).toBeTruthy();
  });
});
