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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { of } from 'rxjs';

import { AuthService } from './auth.service';
import { IdentityProviderService } from './identity-provider.service';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { AppTestingModule, IdentityProviderServiceStub, OAuthServiceStub, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should call log in endpoint', done => {
    const token = { token: 'token', token_type: 'BEARER' };
    service.login('username', 'password').subscribe(resp => {
      expect(resp).toEqual(token);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/login`);

    expect(req.request.headers.get('Authorization')).toEqual('Basic dXNlcm5hbWU6cGFzc3dvcmQ=');
    req.flush(token);
  });

  it('should redirect for SSO', () => {
    const idp = { id: 'google', name: 'Google' };
    const redirectUrl = 'redirectUrl';

    const storeProviderId = jest.spyOn(service, 'storeProviderId').mockReturnValue();
    const initCodeFlow = jest.spyOn(TestBed.inject(OAuthService) as unknown as OAuthServiceStub, 'initCodeFlow').mockReturnValue();

    service.authenticateSSO(idp, redirectUrl);

    expect(initCodeFlow).toHaveBeenCalledWith(redirectUrl);
    expect(storeProviderId).toHaveBeenCalled();
  });

  it('should retrieve token on load from SSO provider', done => {
    const idp = { id: 'google', name: 'Google' } as IdentityProvider;

    const getProviderId = jest.spyOn(service, 'getProviderId').mockReturnValue(idp.id!);
    const getPortalIdentityProvider = jest
      .spyOn(TestBed.inject(IdentityProviderService) as unknown as IdentityProviderServiceStub, 'getPortalIdentityProvider')
      .mockReturnValue(of(idp));
    const tryLoginCodeFlow = jest
      .spyOn(TestBed.inject(OAuthService) as unknown as OAuthServiceStub, 'tryLoginCodeFlow')
      .mockResolvedValue();

    service.load().subscribe(() => {
      expect(getProviderId).toHaveBeenCalled();
      expect(getPortalIdentityProvider).toHaveBeenCalledWith(idp.id);
      expect(tryLoginCodeFlow).toHaveBeenCalled();
      done();
    });
  });
});
