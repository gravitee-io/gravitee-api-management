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
import { clearOidcTransaction, consumeOidcTransaction, isSafeOidcLogoutUrl, storeOidcTransaction } from './oidc-transaction.util';

const OIDC_TRANSACTION_STORAGE_KEY = 'oidc-transaction';
const MOCK_DIGEST = new Uint8Array(32).fill(7);

function encodeBase64Url(bytes: Uint8Array): string {
  const binary = Array.from(bytes, byte => String.fromCodePoint(byte)).join('');

  return btoa(binary).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

describe('oidc-transaction.util', () => {
  beforeEach(() => {
    sessionStorage.clear();
    Object.assign(globalThis.crypto, {
      getRandomValues: (array: Uint8Array) => {
        for (let i = 0; i < array.length; i++) {
          array[i] = i % 256;
        }
        return array;
      },
      subtle: {
        digest: jest.fn().mockResolvedValue(MOCK_DIGEST.buffer),
      },
    });
  });

  describe('storeOidcTransaction', () => {
    it('should persist transaction and return state with PKCE S256 challenge', async () => {
      const result = await storeOidcTransaction({
        providerId: 'google',
        redirectUrl: '/home',
        redirectUri: 'http://localhost',
      });

      expect(result.state).toBeTruthy();
      expect(result.codeChallenge).toBeTruthy();

      const stored = JSON.parse(sessionStorage.getItem(OIDC_TRANSACTION_STORAGE_KEY)!);
      expect(stored).toEqual({
        state: result.state,
        redirectUrl: '/home',
        providerId: 'google',
        redirectUri: 'http://localhost',
        codeVerifier: stored.codeVerifier,
      });
      expect(stored.codeVerifier).toBeTruthy();
      expect(result.codeChallenge).toEqual(encodeBase64Url(MOCK_DIGEST));
    });
  });

  describe('consumeOidcTransaction', () => {
    it('should return null when state is missing', () => {
      expect(consumeOidcTransaction(null)).toBeNull();
      expect(consumeOidcTransaction('')).toBeNull();
    });

    it('should return null when no transaction is stored', () => {
      expect(consumeOidcTransaction('oauth-state')).toBeNull();
    });

    it('should return null when stored transaction is invalid JSON', () => {
      sessionStorage.setItem(OIDC_TRANSACTION_STORAGE_KEY, '{invalid-json');

      expect(consumeOidcTransaction('oauth-state')).toBeNull();
    });

    it('should return null when state does not match', async () => {
      await storeOidcTransaction({
        providerId: 'google',
        redirectUrl: '/home',
        redirectUri: 'http://localhost',
      });

      expect(consumeOidcTransaction('wrong-state')).toBeNull();
      expect(sessionStorage.getItem(OIDC_TRANSACTION_STORAGE_KEY)).toBeTruthy();
    });

    it('should return transaction and clear storage when state matches', async () => {
      const { state } = await storeOidcTransaction({
        providerId: 'google',
        redirectUrl: '/home',
        redirectUri: 'http://localhost',
      });

      const transaction = consumeOidcTransaction(state);

      expect(transaction).toEqual(
        expect.objectContaining({
          state,
          redirectUrl: '/home',
          providerId: 'google',
          redirectUri: 'http://localhost',
        }),
      );
      expect(transaction?.codeVerifier).toBeTruthy();
      expect(sessionStorage.getItem(OIDC_TRANSACTION_STORAGE_KEY)).toBeNull();
    });
  });

  describe('clearOidcTransaction', () => {
    it('should remove stored transaction', async () => {
      await storeOidcTransaction({
        providerId: 'google',
        redirectUrl: '/home',
        redirectUri: 'http://localhost',
      });

      clearOidcTransaction();

      expect(sessionStorage.getItem(OIDC_TRANSACTION_STORAGE_KEY)).toBeNull();
    });
  });

  describe('isSafeOidcLogoutUrl', () => {
    it('should allow https URLs only', () => {
      expect(isSafeOidcLogoutUrl('https://idp.example.com/logout')).toBe(true);
      expect(isSafeOidcLogoutUrl('http://idp.example.com/logout')).toBe(false);
      expect(isSafeOidcLogoutUrl('javascript:alert(1)')).toBe(false);
      expect(isSafeOidcLogoutUrl(undefined)).toBe(false);
    });
  });
});
