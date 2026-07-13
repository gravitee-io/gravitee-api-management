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

function seedOidcTransaction(
  overrides: Partial<{ state: string; redirectUrl: string; providerId: string; redirectUri: string; codeVerifier: string }> = {},
) {
  const transaction = {
    state: 'oauth-state',
    redirectUrl: '/home',
    providerId: 'google',
    redirectUri: 'http://localhost',
    codeVerifier: 'test-code-verifier',
    ...overrides,
  };
  sessionStorage.setItem('oidc-transaction', JSON.stringify(transaction));
  return transaction.state;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    Object.assign(globalThis.crypto, {
      getRandomValues: (array: Uint8Array) => {
        for (let i = 0; i < array.length; i++) {
          array[i] = i % 256;
        }
        return array;
      },
      subtle: {
        digest: jest.fn().mockResolvedValue(new Uint8Array(32).fill(7)),
      },
    });

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

  it('should redirect for SSO', async () => {
    const idp = {
      id: 'google',
      name: 'Google',
      authorizationEndpoint: 'https://idp.example.com/auth',
      client_id: 'client-id',
      scopes: ['openid'],
    };
    const redirectUrl = 'redirectUrl';
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost',
      pathname: '/',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    service.authenticateSSO(idp, redirectUrl);
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(localStorage.getItem('user-provider-id')).toEqual('google');
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('https://idp.example.com/auth'));
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge='));
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge_method=S256'));
    expect(hrefSetter.mock.calls[0][0]).not.toContain('state=redirectUrl');
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

    localStorage.setItem('user-provider-id', 'google');
    jest
      .spyOn(TestBed.inject(IdentityProviderService) as unknown as IdentityProviderServiceStub, 'getPortalIdentityProvider')
      .mockReturnValue(of(idp));
    jest.spyOn(window.history, 'replaceState').mockImplementation(() => {});
    const state = seedOidcTransaction();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: `?code=auth-code&state=${state}`,
      pathname: '/',
      origin: 'http://localhost',
    };

    service.load().subscribe(() => {
      expect(sessionStorage.getItem('oidc-redirect-state')).toEqual('/home');
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/oauth2/google`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toContain('code=auth-code');
    expect(req.request.body).toContain('code_verifier=test-code-verifier');
    req.flush('');

    const userReq = httpTestingController.expectOne(`${TESTING_BASE_URL}/user`);
    userReq.flush({ id: 'user-1' });
  });
});
