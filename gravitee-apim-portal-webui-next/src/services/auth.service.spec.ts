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
import { of } from 'rxjs';

import { AuthService } from './auth.service';
import { IdentityProviderService } from './identity-provider.service';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { AppTestingModule, IdentityProviderServiceStub, TESTING_BASE_URL } from '../testing/app-testing.module';

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

  afterEach(() => {
    httpTestingController.verify();
    sessionStorage.clear();
    localStorage.clear();
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
    const idp = {
      id: 'google',
      name: 'Google',
      authorizationEndpoint: 'https://idp.example.com/auth',
      client_id: 'client-id',
      scopes: ['openid'],
    };
    const redirectUrl = 'redirectUrl';
    const hrefSetter = jest.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        ...window.location,
        origin: 'http://localhost',
        pathname: '/',
        set href(value: string) {
          hrefSetter(value);
        },
      },
    });

    service.authenticateSSO(idp, redirectUrl);

    expect(localStorage.getItem('user-provider-id')).toEqual('google');
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('https://idp.example.com/auth'));
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('state=redirectUrl'));
  });

  it('should call /auth/logout and remove provider id', () => {
    localStorage.setItem('user-provider-id', 'google');

    service.logout().subscribe();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.post_logout_redirect_uri).toBeDefined();
    req.flush({});

    expect(localStorage.getItem('user-provider-id')).toBeNull();
  });

  it('should skip SSO callback when no authorization code is present', done => {
    localStorage.setItem('user-provider-id', 'google');

    service.load().subscribe(() => {
      done();
    });

    httpTestingController.expectNone(`${TESTING_BASE_URL}/auth/oauth2/google`);
  });

  it('should exchange authorization code on load from SSO provider', done => {
    const idp = { id: 'google', name: 'Google', client_id: 'client-id' } as IdentityProvider;

    jest.spyOn(service, 'getProviderId').mockReturnValue(idp.id!);
    jest
      .spyOn(TestBed.inject(IdentityProviderService) as unknown as IdentityProviderServiceStub, 'getPortalIdentityProvider')
      .mockReturnValue(of(idp));

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, search: '?code=auth-code&state=%2Fhome', pathname: '/', origin: 'http://localhost' },
    });

    service.load().subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/oauth2/google`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toContain('code=auth-code');
    req.flush('');
  });
});
