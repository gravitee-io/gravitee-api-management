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

export interface OidcTransaction {
  state: string;
  redirectUrl: string;
  providerId: string;
  redirectUri: string;
  codeVerifier: string;
}

const OIDC_TRANSACTION_STORAGE_KEY = 'oidc-transaction';

export async function storeOidcTransaction(input: {
  providerId: string;
  redirectUrl: string;
  redirectUri: string;
}): Promise<{ state: string; codeChallenge: string }> {
  const codeVerifier = generateCodeVerifier();
  const state = generateRandomState();
  const codeChallenge = await computeS256CodeChallenge(codeVerifier);

  const transaction: OidcTransaction = {
    state,
    redirectUrl: input.redirectUrl,
    providerId: input.providerId,
    redirectUri: input.redirectUri,
    codeVerifier,
  };

  sessionStorage.setItem(OIDC_TRANSACTION_STORAGE_KEY, JSON.stringify(transaction));

  return { state, codeChallenge };
}

export function consumeOidcTransaction(receivedState: string | null): OidcTransaction | null {
  if (!receivedState) {
    return null;
  }

  const raw = sessionStorage.getItem(OIDC_TRANSACTION_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  let transaction: OidcTransaction;
  try {
    transaction = JSON.parse(raw) as OidcTransaction;
  } catch {
    return null;
  }

  if (transaction.state !== receivedState) {
    return null;
  }

  sessionStorage.removeItem(OIDC_TRANSACTION_STORAGE_KEY);
  return transaction;
}

export function clearOidcTransaction(): void {
  sessionStorage.removeItem(OIDC_TRANSACTION_STORAGE_KEY);
}

function generateCodeVerifier(): string {
  const random = new Uint8Array(32);
  crypto.getRandomValues(random);
  return base64UrlEncode(random);
}

function generateRandomState(): string {
  const random = new Uint8Array(16);
  crypto.getRandomValues(random);
  return base64UrlEncode(random);
}

async function computeS256CodeChallenge(codeVerifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier));
  return base64UrlEncode(new Uint8Array(digest));
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  bytes.forEach(byte => {
    binary += String.fromCharCode(byte);
  });

  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
