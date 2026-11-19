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
import { Router } from '@angular/router';

import { LogInComponent } from './log-in.component';
import { IdentityProvider } from '../../entities/configuration/identity-provider';
import { fakePortalNavigationLink } from '../../entities/portal-navigation/portal-navigation-item.fixture';
import { fakeUser } from '../../entities/user/user.fixtures';
import { AuthService } from '../../services/auth.service';
import { ConfigService } from '../../services/config.service';
import { CurrentUserService } from '../../services/current-user.service';
import { IdentityProviderService } from '../../services/identity-provider.service';
import { PortalNavigationItemsService } from '../../services/portal-navigation-items.service';
import {
  AppTestingModule,
  ConfigServiceStub,
  IdentityProviderServiceStub,
  TESTING_BASE_URL,
  TESTING_PORTAL_BASE_URL,
} from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('LogInComponent', () => {
  let fixture: ComponentFixture<LogInComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (
    params: Partial<{ enableLocalLogin: boolean; enableRegistration: boolean; ssoProviders: IdentityProvider[] }> = {
      enableLocalLogin: true,
      ssoProviders: [],
    },
  ) => {
    await TestBed.configureTestingModule({
      imports: [LogInComponent, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useFactory: () => {
            const stub = new ConfigServiceStub();
            stub.configuration.authentication!.localLogin!.enabled = params.enableLocalLogin;
            stub.configuration.portal = { userCreation: { enabled: params.enableRegistration ?? true } };
            return stub;
          },
        },
        {
          provide: IdentityProviderService,
          useFactory: () => {
            const stub = new IdentityProviderServiceStub();
            stub.providers = params.ssoProviders!;
            return stub;
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogInComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should allow submit if username and password are valid', async () => {
    await init();

    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');

    expect(await submitButton.isDisabled()).toEqual(true);

    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(false);
  });

  it('should not validate form with missing username', async () => {
    await init();

    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(true);
  });

  it('should not validate form with missing password', async () => {
    await init();

    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));

    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');

    expect(await submitButton.isDisabled()).toEqual(true);
  });

  it('should login and fetch current user on submit', async () => {
    await init();

    const submitButton = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Log in' }));
    const username = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="username"]' }));
    await username.setValue('john@doe.com');
    const password = await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="password"]' }));
    await password.setValue('password');

    expect(await submitButton.isDisabled()).toEqual(false);
    await submitButton.click();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`).flush({});
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user`).flush({});
    httpTestingController.expectOne(`${TESTING_PORTAL_BASE_URL}/portal-navigation-items?area=TOP_NAVBAR&loadChildren=false`).flush({});
  });

  it.each([401, 404, 500])('should complete login and clear stale navigation when navbar reload fails with %p', async status => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const navigation = TestBed.inject(PortalNavigationItemsService);
    navigation.topNavbarItems.set([fakePortalNavigationLink()]);
    const user = fakeUser();

    fixture.componentInstance.logInForm.setValue({ username: 'synthetic-user', password: 'synthetic-password' });
    fixture.componentInstance.logIn();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`).flush({});
    httpTestingController.expectOne(`${TESTING_BASE_URL}/user`).flush(user);
    httpTestingController
      .expectOne(`${TESTING_PORTAL_BASE_URL}/portal-navigation-items?area=TOP_NAVBAR&loadChildren=false`)
      .flush({}, { status, statusText: 'Navigation reload failed' });

    expect(TestBed.inject(CurrentUserService).user()).toEqual(user);
    expect(navigation.topNavbarItems()).toEqual([]);
    expect(navigate).toHaveBeenCalledWith(['']);
    const form = await harnessLoader.getHarness(DivHarness.with({ selector: '.log-in__form' }));
    expect(await form.getText({ childSelector: '.log-in__form__error' })).toBeNull();
  });

  it('should display invalid credentials without requesting navigation or redirecting', async () => {
    await init();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.logInForm.setValue({ username: 'synthetic-user', password: 'synthetic-password' });
    fixture.componentInstance.logIn();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });

    httpTestingController.expectNone(`${TESTING_BASE_URL}/user`);
    httpTestingController.expectNone(request => request.url.includes('/portal-navigation-items'));
    expect(navigate).not.toHaveBeenCalled();
    expect(TestBed.inject(CurrentUserService).isAuthenticated()).toBe(false);
    const form = await harnessLoader.getHarness(DivHarness.with({ selector: '.log-in__form' }));
    expect(await form.getText({ childSelector: '.log-in__form__error' })).toContain('The username or password you entered is incorrect');
  });

  it('should display sign up link when user registration is enabled', async () => {
    await init({ enableLocalLogin: true, enableRegistration: true, ssoProviders: [] });

    const registration = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__registration' }));
    expect(registration).not.toBeNull();
  });

  it('should not display sign up link when user registration is disabled', async () => {
    await init({ enableLocalLogin: true, enableRegistration: false, ssoProviders: [] });

    const registration = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__registration' }));
    expect(registration).toBeNull();
  });

  it('should not display log-in form', async () => {
    await init({
      enableLocalLogin: false,
      ssoProviders: [
        {
          id: 'github',
          name: 'GitHub',
        },
      ],
    });

    const login = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__form' }));
    expect(login).toBeNull();

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__or-separator' }));
    expect(orSeparator).toBeNull();

    const ssoProvider = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.log-in__sso-provider' }));
    expect(ssoProvider).not.toBeNull();

    const providerText = await ssoProvider!.getText();
    expect(providerText).toEqual('Continue with GitHub');
  });

  it('should not display SSO providers', async () => {
    await init({
      enableLocalLogin: true,
    });

    const login = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__form' }));
    expect(login).not.toBeNull();

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__or-separator' }));
    expect(orSeparator).toBeNull();

    const ssoProvider = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.log-in__sso-provider' }));
    expect(ssoProvider).toBeNull();
  });

  it('should display "or" separator and identity providers', async () => {
    const identityProviders = [
      { id: 'github', name: 'GitHub' },
      { id: 'google', name: 'Google' },
      { id: 'graviteeio_am', name: 'Gravitee AM' },
    ];
    await init({
      enableLocalLogin: true,
      ssoProviders: identityProviders,
    });

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__or-separator' }));
    expect(orSeparator).not.toBeNull();

    const ssoProviders = await harnessLoader.getAllHarnesses(MatButtonHarness.with({ selector: '.log-in__sso-provider' }));
    const providerTexts = await Promise.all(ssoProviders.map(harness => harness.getText()));
    console.log('providerTexts', providerTexts);

    for (const provider of identityProviders) {
      const found = providerTexts.find(text => text === `Continue with ${provider.name}`);
      expect(found).toBeDefined();
    }
  });

  it('should redirect when clicked on SSO provider', async () => {
    await init({
      enableLocalLogin: true,
      ssoProviders: [{ id: 'google', name: 'Google' }],
    });
    const authenticateSSO = jest.spyOn(TestBed.inject(AuthService), 'authenticateSSO').mockReturnValue();

    const ssoProvider = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.log-in__sso-provider' }));
    await ssoProvider.click();
    expect(authenticateSSO).toHaveBeenCalledWith({ id: 'google', name: 'Google' }, '');
  });
});
