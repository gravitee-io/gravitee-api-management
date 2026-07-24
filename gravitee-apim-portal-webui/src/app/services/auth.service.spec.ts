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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { AuthenticationService, PortalService } from '../../../projects/portal-webclient-sdk/src/lib';

import { AuthService } from './auth.service';
import { ConfigurationService } from './configuration.service';
import { CurrentUserService } from './current-user.service';
import { NotificationService } from './notification.service';

const BASE_URL = 'http://localhost:8083/portal/environments/DEFAULT';

function seedOidcTransaction(
  overrides: Partial<{ state: string; redirectUrl: string; providerId: string; redirectUri: string; codeVerifier: string }> = {},
) {
  const transaction = {
    state: 'oauth-state',
    redirectUrl: '/home',
    providerId: 'google',
    redirectUri: 'http://localhost:4000',
    codeVerifier: 'test-code-verifier',
    ...overrides,
  };
  sessionStorage.setItem('oidc-transaction', JSON.stringify(transaction));
  return transaction.state;
}

describe('AuthService', () => {
  let spectator: SpectatorService<AuthService>;
  let httpTestingController: HttpTestingController;

  const createService = createServiceFactory({
    service: AuthService,
    imports: [HttpClientTestingModule, RouterTestingModule],
    mocks: [ConfigurationService, CurrentUserService, NotificationService, PortalService, AuthenticationService],
    providers: [],
  });

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

    spectator = createService({
      providers: [
        {
          provide: ConfigurationService,
          useValue: {
            get: (key: string) => (key === 'baseURL' ? BASE_URL : undefined),
          },
        },
      ],
    });
    httpTestingController = spectator.inject(HttpTestingController);
    spectator
      .inject(PortalService)
      .getPortalIdentityProvider.mockReturnValue(of({ id: 'google', client_id: 'client-id', scopes: ['openid'] }) as any);
  });

  afterEach(() => {
    httpTestingController.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('should redirect for SSO with PKCE', async () => {
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost:4000',
      pathname: '/',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    spectator.service.authenticate(
      {
        id: 'google',
        authorizationEndpoint: 'https://idp.example.com/auth',
        client_id: 'client-id',
        scopes: ['openid'],
      },
      '/catalog',
    );

    await new Promise(resolve => setTimeout(resolve, 0));

    expect(localStorage.getItem('user-provider-id')).toEqual('google');
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('https://idp.example.com/auth'));
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge='));
    expect(hrefSetter).toHaveBeenCalledWith(expect.stringContaining('code_challenge_method=S256'));
    expect(hrefSetter.mock.calls[0][0]).not.toContain('state=/catalog');
  });

  it('should exchange authorization code on load from SSO provider', async () => {
    localStorage.setItem('user-provider-id', 'google');
    jest.spyOn(window.history, 'replaceState').mockImplementation(() => {});
    const state = seedOidcTransaction();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: `?code=auth-code&state=${state}`,
      pathname: '/',
      origin: 'http://localhost:4000',
    };

    const currentUserService = spectator.inject(CurrentUserService);
    currentUserService.load.mockResolvedValue(true);
    currentUserService.getUser.mockReturnValue({ id: 'user-1' });

    const loadPromise = spectator.service.completeOidcLoginIfPresent();

    await new Promise(resolve => setTimeout(resolve, 0));

    const oauthReq = httpTestingController.expectOne(`${BASE_URL}/auth/oauth2/google`);
    expect(oauthReq.request.method).toBe('POST');
    expect(oauthReq.request.body).toContain('code=auth-code');
    expect(oauthReq.request.body).toContain('code_verifier=test-code-verifier');
    oauthReq.flush('');

    await loadPromise;

    expect(sessionStorage.getItem('oidc-redirect-state')).toEqual('/home');
    expect(currentUserService.load).toHaveBeenCalled();
  });

  it('should reject SSO callback with invalid state', async () => {
    localStorage.setItem('user-provider-id', 'google');
    jest.spyOn(window.history, 'replaceState').mockImplementation(() => {});

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      search: '?code=auth-code&state=invalid-state',
      pathname: '/',
      origin: 'http://localhost:4000',
    };

    await expect(spectator.service.completeOidcLoginIfPresent()).rejects.toThrow('Invalid OIDC state');
    expect(localStorage.getItem('user-provider-id')).toBeNull();
    httpTestingController.expectNone(`${BASE_URL}/auth/oauth2/google`);
  });

  it('should call /auth/logout and navigate home when no logout_url is returned', async () => {
    const router = spectator.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    const currentUserService = spectator.inject(CurrentUserService);

    const logoutPromise = spectator.service.logout();

    const req = httpTestingController.expectOne(`${BASE_URL}/auth/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.post_logout_redirect_uri).toBeDefined();
    req.flush({});

    await logoutPromise;

    expect(currentUserService.revokeUser).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['']);
  });

  it('should redirect to IdP logout URL when returned by server', async () => {
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost:4000',
      pathname: '/',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    const logoutPromise = spectator.service.logout();

    const req = httpTestingController.expectOne(`${BASE_URL}/auth/logout`);
    req.flush({ logout_url: 'https://idp.example.com/logout' });

    await logoutPromise;

    expect(hrefSetter).toHaveBeenCalledWith('https://idp.example.com/logout');
  });

  it('should skip non-https logout_url and navigate home', async () => {
    const router = spectator.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    const hrefSetter = jest.fn();

    delete (window as unknown as { location?: Location }).location;
    (window as unknown as { location: Partial<Location> }).location = {
      origin: 'http://localhost:4000',
      pathname: '/',
    };
    Object.defineProperty(window.location, 'href', {
      configurable: true,
      set: hrefSetter,
      get: () => '',
    });

    const logoutPromise = spectator.service.logout();

    const req = httpTestingController.expectOne(`${BASE_URL}/auth/logout`);
    req.flush({ logout_url: 'http://idp.example.com/logout' });

    await logoutPromise;

    expect(hrefSetter).not.toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['']);
  });
});
