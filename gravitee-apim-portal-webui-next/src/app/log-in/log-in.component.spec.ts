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
import { IdentityProvider } from '../../entities/configuration/identity-provider';
import { AuthService } from '../../services/auth.service';
import { ConfigService } from '../../services/config.service';
import { IdentityProviderService } from '../../services/identity-provider.service';
import { AppTestingModule, ConfigServiceStub, IdentityProviderServiceStub, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { DivHarness } from '../../testing/div.harness';

describe('LogInComponent', () => {
  let fixture: ComponentFixture<LogInComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  async function initComponent() {
    fixture = TestBed.createComponent(LogInComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function enableLocalLogin(enable: boolean) {
    const configService = TestBed.inject(ConfigService) as unknown as ConfigServiceStub;
    configService.configuration.authentication!.localLogin!.enabled = enable;
  }

  function initSsoProviders(providers: IdentityProvider[]) {
    const identityProviderService = TestBed.inject(IdentityProviderService) as unknown as IdentityProviderServiceStub;
    identityProviderService.providers = providers;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogInComponent, AppTestingModule],
    }).compileComponents();

    await initComponent();
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

  it('should not display log-in form', async () => {
    enableLocalLogin(false);
    initSsoProviders([
      {
        id: 'github',
        name: 'GitHub',
      },
    ]);

    await initComponent();

    const login = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__form' }));
    expect(login).toBeNull();

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__sso__separator' }));
    expect(orSeparator).toBeNull();

    const ssoProvider = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.log-in__sso__idp' }));
    expect(ssoProvider).not.toBeNull();

    const providerText = await ssoProvider!.getText();
    expect(providerText).toEqual('Continue with GitHub');
  });

  it('should not display SSO providers', async () => {
    enableLocalLogin(true);
    initSsoProviders([]);

    await initComponent();

    const login = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__form' }));
    expect(login).not.toBeNull();

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__sso__separator' }));
    expect(orSeparator).toBeNull();

    const ssoProvider = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '.log-in__sso__idp' }));
    expect(ssoProvider).toBeNull();
  });

  it('should display "or" separator and identity providers', async () => {
    const identityProviders = [
      { id: 'github', name: 'GitHub' },
      { id: 'google', name: 'Google' },
      { id: 'graviteeio_am', name: 'Gravitee AM' },
    ];
    enableLocalLogin(true);
    initSsoProviders(identityProviders);

    await initComponent();

    const orSeparator = await harnessLoader.getHarnessOrNull(DivHarness.with({ selector: '.log-in__sso__separator' }));
    expect(orSeparator).not.toBeNull();

    const ssoProviders = await harnessLoader.getAllHarnesses(MatButtonHarness.with({ selector: '.log-in__sso__idp' }));
    const providerTexts = await Promise.all(ssoProviders.map(harness => harness.getText()));
    console.log('providerTexts', providerTexts);

    for (const provider of identityProviders) {
      const found = providerTexts.find(text => text === `Continue with ${provider.name}`);
      expect(found).toBeDefined();
    }
  });

  it('should redirect when clicked on SSO provider', async () => {
    enableLocalLogin(true);
    initSsoProviders([{ id: 'google', name: 'Google' }]);

    const authenticateSSO = jest.spyOn(TestBed.inject(AuthService), 'authenticateSSO').mockReturnValue();

    await initComponent();

    const ssoProvider = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '.log-in__sso__idp' }));
    await ssoProvider.click();
    expect(authenticateSSO).toHaveBeenCalledWith({ id: 'google', name: 'Google' }, '');
  });
});
