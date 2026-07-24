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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { Constants } from '../entities/Constants';
import { SocialIdentityProvider } from '../entities/organization/socialIdentityProvider';

const IDENTITY_PROVIDER: SocialIdentityProvider = {
  id: 'google',
  name: 'Google',
  clientId: 'client-id',
  authorizationEndpoint: 'https://idp.example.com/auth',
  scopes: ['openid'],
};

const BASE_URL = CONSTANTS_TESTING.org!.baseURL!;

function seedOidcTransaction(
  overrides: Partial<{ state: string; redirectUrl: string; providerId: string; redirectUri: string; codeVerifier: string }> = {},
) {
  const transaction = {
    state: 'oauth-state',
    redirectUrl: '/home',
    providerId: 'google',
    redirectUri: 'http://localhost/console',
    codeVerifier: 'test-code-verifier',
    ...overrides,
  };
  sessionStorage.setItem('oidc-transaction', JSON.stringify(transaction));
  return transaction.state;
}

const CONSTANTS_WITH_IDP: Constants = {
  ...CONSTANTS_TESTING,
  org: {
    ...CONSTANTS_TESTING.org!,
    identityProviders: [IDENTITY_PROVIDER],
  },
};

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;
  let router: Router;

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
      imports: [GioTestingModule],
      providers: [{ provide: Constants, useValue: CONSTANTS_WITH_IDP }],
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTestingController.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('should redirect for SSO', done => {
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost',
      pathname: '/console',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    service.loginWithProvider('google', 'redirectUrl').subscribe(() => {
      expect(localStorage.getItem('user-provider-id-selected')).toEqual('google');
      expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('https://idp.example.com/auth'));
      expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge='));
      expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge_method=S256'));
      expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('client_id=client-id'));
      expect(hrefSetter.mock.calls[0][0]).not.toContain('state=redirectUrl');
      done();
    });
  });

  it('should throw when SSO provider is not found', () => {
    expect(() => service.loginWithProvider('unknown', '/')).toThrow('Identity provider unknown not found!');
  });

  it('should skip SSO callback when no authorization code is present', done => {
    localStorage.setItem('user-provider-id-selected', 'google');

    service.checkAuth().subscribe(() => {
      done();
    });

    httpTestingController.expectNone(`${BASE_URL}/auth/oauth2/google`);
  });

  it('should skip SSO callback when authorization code was already processed', done => {
    localStorage.setItem('user-provider-id-selected', 'google');
    sessionStorage.setItem('oidc-code-processed:auth-code', '1');

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: '?code=auth-code&state=%2Fhome',
      pathname: '/',
      origin: 'http://localhost',
    };

    service.checkAuth().subscribe(() => {
      done();
    });

    httpTestingController.expectNone(`${BASE_URL}/auth/oauth2/google`);
  });

  it('should exchange authorization code on checkAuth from SSO provider', done => {
    localStorage.setItem('user-provider-id-selected', 'google');
    sessionStorage.setItem('oidc-redirect-uri', 'http://localhost/console');
    jest.spyOn(window.history, 'replaceState').mockImplementation(() => {});
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const state = seedOidcTransaction();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: `?code=auth-code&state=${state}`,
      pathname: '/console',
      origin: 'http://localhost',
    };

    service.checkAuth().subscribe(() => {
      expect(navigateSpy).toHaveBeenCalledWith('/home', { replaceUrl: true });
      expect(sessionStorage.getItem('oidc-code-processed:auth-code')).toEqual('1');
      expect(sessionStorage.getItem('oidc-redirect-uri')).toBeNull();
      done();
    });

    const oauthReq = httpTestingController.expectOne(`${BASE_URL}/auth/oauth2/google`);
    expect(oauthReq.request.method).toBe('POST');
    expect(oauthReq.request.body).toContain('code=auth-code');
    expect(oauthReq.request.body).toContain('client_id=client-id');
    expect(oauthReq.request.body).toContain('code_verifier=test-code-verifier');
    oauthReq.flush('');

    const userReq = httpTestingController.expectOne(`${BASE_URL}/user`);
    userReq.flush({ id: 'user-1' });
  });

  it('should call /user/logout and navigate to login', done => {
    localStorage.setItem('user-provider-id-selected', 'google');
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.logout().subscribe(() => {
      expect(navigateSpy).toHaveBeenCalledWith('/_login');
      expect(localStorage.getItem('user-provider-id-selected')).toBeNull();
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.post_logout_redirect_uri).toContain('/_login');
    req.flush({});
  });

  it('should redirect to IdP logout URL when returned by server', () => {
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost',
      pathname: '/console',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    service.logout().subscribe();

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    req.flush({ logout_url: 'https://idp.example.com/logout' });

    expect(hrefSetter).toHaveBeenCalledWith('https://idp.example.com/logout');
  });

  it('should skip non-https logout_url and navigate to login', done => {
    const hrefSetter = jest.fn();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost',
      pathname: '/console',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    service.logout().subscribe(() => {
      expect(hrefSetter).not.toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith('/_login');
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    req.flush({ logout_url: 'http://idp.example.com/logout' });
  });

  it('should not redirect on logout when disableRedirect is set', done => {
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.logout({ disableRedirect: true }).subscribe(() => {
      expect(navigateSpy).not.toHaveBeenCalled();
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    req.flush({});
  });

  it('should login with username and password', done => {
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.loginWithApim({ username: 'username', password: 'password' }, '/home').subscribe(() => {
      expect(navigateSpy).toHaveBeenCalledWith('/home');
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/login`);
    expect(req.request.headers.get('Authorization')).toEqual('Basic dXNlcm5hbWU6cGFzc3dvcmQ=');
    req.flush(null);
  });

  it('should reject SSO callback with invalid state', done => {
    localStorage.setItem('user-provider-id-selected', 'google');
    jest.spyOn(window.history, 'replaceState').mockImplementation(() => {});

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: '?code=auth-code&state=invalid-state',
      pathname: '/console',
      origin: 'http://localhost',
    };

    service.checkAuth().subscribe({
      error: () => {
        expect(localStorage.getItem('user-provider-id-selected')).toBeNull();
        httpTestingController.expectNone(`${BASE_URL}/auth/oauth2/google`);
        done();
      },
    });
  });

  it('should redirect to login with current url on logout', done => {
    Object.defineProperty(router, 'routerState', {
      configurable: true,
      value: { snapshot: { url: '/apis' } },
    });
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.logout({ redirectToCurrentUrl: true }).subscribe(() => {
      expect(navigateSpy).toHaveBeenCalledWith('/_login?redirect=/apis');
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    req.flush({});
  });

  it('should complete logout when server returns an error', done => {
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    service.logout().subscribe(() => {
      expect(navigateSpy).toHaveBeenCalledWith('/_login');
      done();
    });

    const req = httpTestingController.expectOne(`${BASE_URL}/user/logout`);
    req.flush('Server error', { status: 500, statusText: 'Server error' });
  });
});
