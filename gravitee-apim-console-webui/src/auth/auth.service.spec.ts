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
import { HttpClient } from '@angular/common/http';
import { LocationStrategy } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { OidcClient, SigninRequest, SignoutRequest, UserManager } from 'oidc-client-ts';

import { randomFillSync } from 'crypto';

import { AuthService, OpenIdScopeFilteringStateStore } from './auth.service';

import { Constants } from '../entities/Constants';
import { CurrentUserService } from '../services-ngx/current-user.service';

const BASE_URL = 'https://apim.example.com/management/organizations/DEFAULT';
const IDP_ID = 'oidc-idp';

describe('AuthService', () => {
  const navigateByUrl = jest.fn().mockResolvedValue(true);
  let fetchMock: jest.Mock;

  beforeAll(() => {
    // The jsdom crypto object has no getRandomValues, which oidc-client-ts needs for the state and PKCE verifier.
    // The object is patched in place because crypto-js keeps the reference it read when it was loaded.
    Object.defineProperty(window.crypto, 'getRandomValues', {
      value: (array: Uint32Array): Uint32Array => randomFillSync(array),
      configurable: true,
    });
  });

  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/');
    navigateByUrl.mockClear();
    fetchMock = jest.fn();
    window.fetch = fetchMock;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  afterAll(() => {
    delete (window.crypto as { getRandomValues?: unknown }).getRandomValues;
  });

  const createService = (exposeIdToken?: boolean): AuthService => {
    const constants = {
      org: {
        baseURL: BASE_URL,
        settings: exposeIdToken === undefined ? {} : { authentication: { exposeIdToken: { enabled: exposeIdToken } } },
        identityProviders: [
          {
            id: IDP_ID,
            name: 'OIDC',
            type: 'OIDC',
            clientId: 'console-client',
            authorizationEndpoint: 'https://idp.example.com/oauth/authorize',
            userLogoutEndpoint: 'https://idp.example.com/logout',
            scopes: ['openid', 'profile'],
          },
        ],
      },
    } as unknown as Constants;

    return new AuthService(
      constants,
      { post: jest.fn().mockReturnValue(of({})) } as unknown as HttpClient,
      { navigateByUrl } as unknown as Router,
      { clearCurrent: jest.fn() } as unknown as CurrentUserService,
      { prepareExternalUrl: (url: string) => url } as unknown as LocationStrategy,
    );
  };

  const tokenResponse = (body: Record<string, unknown>) => ({
    ok: true,
    status: 200,
    statusText: 'OK',
    headers: { get: () => 'application/json' },
    text: () => Promise.resolve(JSON.stringify(body)),
  });

  const idToken = (claims: Record<string, unknown>): string =>
    ['{"alg":"none"}', JSON.stringify(claims)].map(part => btoa(part).replace(/=+$/, '')).join('.') + '.signature';

  const pendingSigninStateId = async (): Promise<string> => {
    for (let attempt = 0; attempt < 50; attempt++) {
      const key = Object.keys(localStorage).find(k => k.startsWith('oidc.') && !k.startsWith('oidc.user:'));
      if (key) {
        return key.substring('oidc.'.length);
      }
      await new Promise(resolve => setTimeout(resolve, 0));
    }
    throw new Error('No signin state was stored');
  };

  const signoutRequest = async (createSignoutRequest: jest.SpyInstance): Promise<SignoutRequest> => {
    for (let attempt = 0; attempt < 50 && createSignoutRequest.mock.results.length === 0; attempt++) {
      await new Promise(resolve => setTimeout(resolve, 0));
    }
    return createSignoutRequest.mock.results[0].value;
  };

  /**
   * Starts the login (stores the signin state the way the browser would), then lands on the redirect URI with an
   * authorization code, as the identity provider does after a successful sign-in.
   */
  const signInWithProvider = async (service: AuthService, body: Record<string, unknown>): Promise<void> => {
    // The redirect to the identity provider never resolves in a test; only the stored state matters.
    void service.loginWithProvider(IDP_ID, '/home').subscribe({ error: () => undefined });
    const stateId = await pendingSigninStateId();
    fetchMock.mockResolvedValue(tokenResponse(body));
    window.history.replaceState({}, '', `/?code=authorization-code&state=${stateId}`);

    await firstValueFrom(service.checkAuth());
  };

  describe('when the ID token is not exposed', () => {
    it('should log in with the empty id_token the Management API returns', async () => {
      const service = createService(false);

      await signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: '', id_token: '' });

      expect(fetchMock).toHaveBeenCalledWith(`${BASE_URL}/auth/oauth2/${IDP_ID}`, expect.anything());
      expect(navigateByUrl).toHaveBeenCalledWith('/home');
    });

    it('should log in when the response carries no id_token at all', async () => {
      const service = createService(false);

      await signInWithProvider(service, { token: 'session', type: 'BEARER' });

      expect(navigateByUrl).toHaveBeenCalledWith('/home');
    });

    it('should still request the openid scope from the identity provider', async () => {
      const service = createService(false);
      const createSigninRequest = jest.spyOn(OidcClient.prototype, 'createSigninRequest');

      void service.loginWithProvider(IDP_ID, '/home').subscribe({ error: () => undefined });
      await pendingSigninStateId();

      const request: SigninRequest = await createSigninRequest.mock.results[0].value;
      expect(new URL(request.url).searchParams.get('scope')).toEqual('openid profile');
    });

    it('should keep the user logged in on the next check', async () => {
      const service = createService(false);
      await signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: '', id_token: '' });
      fetchMock.mockClear();
      navigateByUrl.mockClear();
      window.history.replaceState({}, '', '/');

      await firstValueFrom(service.checkAuth());

      expect(fetchMock).not.toHaveBeenCalled();
      expect(navigateByUrl).not.toHaveBeenCalled();
    });

    it('should sign out at the provider with the client_id and without an id_token_hint', async () => {
      const service = createService(false);
      await signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: '', id_token: '' });
      // jsdom reports the redirect to the end-session endpoint as an unimplemented navigation.
      jest.spyOn(console, 'error').mockImplementation(() => undefined);
      const createSignoutRequest = jest.spyOn(OidcClient.prototype, 'createSignoutRequest');

      await firstValueFrom(service.logout());

      const endSessionUrl = new URL((await signoutRequest(createSignoutRequest)).url);
      expect(endSessionUrl.origin + endSessionUrl.pathname).toEqual('https://idp.example.com/logout');
      expect(endSessionUrl.searchParams.get('client_id')).toEqual('console-client');
      expect(endSessionUrl.searchParams.has('id_token_hint')).toBe(false);
    });

    it('should not send an ID token as id_token_hint on sign out', async () => {
      const service = createService(false);
      await signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: '', id_token: idToken({ sub: 'user-1' }) });
      const signoutRedirect = jest.spyOn(UserManager.prototype, 'signoutRedirect').mockResolvedValue(undefined);

      await firstValueFrom(service.logout());

      expect(signoutRedirect).toHaveBeenCalledWith({});
    });
  });

  describe('when the ID token is exposed', () => {
    it('should still require a valid ID token', async () => {
      const service = createService(true);

      await expect(signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: '', id_token: '' })).rejects.toThrow();
      expect(navigateByUrl).not.toHaveBeenCalled();
    });

    it('should log in and send the ID token as id_token_hint on sign out', async () => {
      const service = createService();
      const token = idToken({ sub: 'user-1' });
      await signInWithProvider(service, { token: 'session', type: 'BEARER', access_token: 'at', id_token: token });
      expect(navigateByUrl).toHaveBeenCalledWith('/home');
      const signoutRedirect = jest.spyOn(UserManager.prototype, 'signoutRedirect').mockResolvedValue(undefined);

      await firstValueFrom(service.logout());

      expect(signoutRedirect).toHaveBeenCalledWith({ id_token_hint: token });
    });
  });
});

describe('OpenIdScopeFilteringStateStore', () => {
  let backingStore: Record<string, string>;
  let store: OpenIdScopeFilteringStateStore;

  beforeEach(() => {
    backingStore = {};
    const fakeStorage = {
      getItem: (key: string) => backingStore[key] ?? null,
      setItem: (key: string, value: string) => {
        backingStore[key] = value;
      },
    } as unknown as Storage;
    store = new OpenIdScopeFilteringStateStore({ store: fakeStorage });
  });

  it('removes the openid scope from a serialised sign-in state and keeps the other scopes and fields', async () => {
    await store.set('state-id', JSON.stringify({ id: 'state-id', scope: 'profile openid email', code_verifier: 'verifier' }));

    expect(JSON.parse(backingStore['oidc.state-id'])).toEqual({ id: 'state-id', scope: 'profile email', code_verifier: 'verifier' });
  });

  it('stores a state without scope unchanged', async () => {
    const signoutState = JSON.stringify({ id: 'state-id', request_type: 'so:r' });

    await store.set('state-id', signoutState);

    expect(backingStore['oidc.state-id']).toEqual(signoutState);
  });

  it('stores a JSON value that is not an object unchanged', async () => {
    await store.set('state-id', 'null');

    expect(backingStore['oidc.state-id']).toEqual('null');
  });

  it('stores non-JSON values unchanged', async () => {
    await store.set('state-id', 'not-json');

    expect(backingStore['oidc.state-id']).toEqual('not-json');
  });
});
