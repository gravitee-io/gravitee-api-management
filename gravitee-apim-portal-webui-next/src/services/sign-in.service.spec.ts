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
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { ConfigService } from './config.service';
import { IdentityProviderService } from './identity-provider.service';
import { SignInService } from './sign-in.service';
import { Configuration } from '../entities/configuration/configuration';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { AppTestingModule, ConfigServiceStub, IdentityProviderServiceStub } from '../testing/app-testing.module';

const GOOGLE: IdentityProvider = { id: 'google', name: 'Google' };
const GITHUB: IdentityProvider = { id: 'github', name: 'GitHub' };

describe('SignInService', () => {
  let service: SignInService;
  let configService: ConfigServiceStub;
  let identityProviderService: IdentityProviderServiceStub;
  let authenticateSSO: jest.SpyInstance;
  let navigate: jest.SpyInstance;

  const eligibleConfiguration: Configuration = {
    portalNext: { skipLoginWithSingleIdp: { enabled: true } },
    authentication: { localLogin: { enabled: false } },
    portal: { userCreation: { enabled: false } },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [SignInService],
    });
    service = TestBed.inject(SignInService);
    configService = TestBed.inject(ConfigService) as unknown as ConfigServiceStub;
    identityProviderService = TestBed.inject(IdentityProviderService) as unknown as IdentityProviderServiceStub;
    authenticateSSO = jest.spyOn(TestBed.inject(AuthService), 'authenticateSSO').mockReturnValue();
    navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    configService.configuration = eligibleConfiguration;
    identityProviderService.providers = [GOOGLE];
  });

  describe('when every condition holds', () => {
    it('should start SSO with the only provider instead of opening the login page', () => {
      service.signIn();

      expect(authenticateSSO).toHaveBeenCalledWith(GOOGLE, '');
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('when a condition does not hold', () => {
    it.each([
      ['the setting is off', { ...eligibleConfiguration, portalNext: { skipLoginWithSingleIdp: { enabled: false } } }],
      ['the setting is missing', { ...eligibleConfiguration, portalNext: {} }],
      ['the portal next configuration is missing', { ...eligibleConfiguration, portalNext: undefined }],
      ['the login form is on', { ...eligibleConfiguration, authentication: { localLogin: { enabled: true } } }],
      ['registration is on', { ...eligibleConfiguration, portal: { userCreation: { enabled: true } } }],
    ])('should open the login page when %s', (_, configuration: Configuration) => {
      configService.configuration = configuration;

      service.signIn();

      expect(authenticateSSO).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/log-in']);
    });

    it.each([
      ['there is no provider', []],
      ['there are several providers', [GOOGLE, GITHUB]],
    ])('should open the login page when %s', (_, providers: IdentityProvider[]) => {
      identityProviderService.providers = providers;

      service.signIn();

      expect(authenticateSSO).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/log-in']);
    });
  });

  describe('counting the providers', () => {
    it('should use the full count, not the size of the first page', () => {
      identityProviderService.providers = [GOOGLE];
      identityProviderService.total = 2;

      service.signIn();

      expect(authenticateSSO).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/log-in']);
    });

    it('should open the login page when the count is missing', () => {
      jest.spyOn(identityProviderService, 'getPortalIdentityProviders').mockReturnValue(of({ data: [GOOGLE] }));

      service.signIn();

      expect(authenticateSSO).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/log-in']);
    });
  });

  describe('when the provider list cannot be read', () => {
    it('should open the login page', () => {
      jest.spyOn(identityProviderService, 'getPortalIdentityProviders').mockReturnValue(throwError(() => new Error('nope')));

      service.signIn();

      expect(authenticateSSO).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/log-in']);
    });
  });

  describe('repeated clicks', () => {
    it('should start the sign-in only once while it is in progress', () => {
      service.signIn();
      service.signIn();

      expect(authenticateSSO).toHaveBeenCalledTimes(1);
    });

    it('should accept a new sign-in once the page is restored from the back-forward cache', () => {
      service.signIn();

      window.dispatchEvent(new Event('pageshow'));
      service.signIn();

      expect(authenticateSSO).toHaveBeenCalledTimes(2);
    });
  });
});
