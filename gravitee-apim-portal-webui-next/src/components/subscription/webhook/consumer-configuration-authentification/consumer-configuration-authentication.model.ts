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
import { WebhookSubscriptionConfigurationAuthType } from '../../../../entities/subscription';

export type AuthFormValue = {
  type: WebhookSubscriptionConfigurationAuthType;
  username?: string;
  password?: string;
  token?: string;
  endpoint?: string;
  clientId?: string;
  clientSecret?: string;
  scopes?: string[];
  issuer?: string;
  subject?: string;
  audience?: string;
  expirationTime?: number;
  expirationTimeUnit?: string;
  signatureAlgorithm?: string;
  keySource?: string;
  jwtId?: string;
  secretBase64Encoded?: boolean;
  x509CertChain?: string;
  alias?: string;
  storePassword?: string;
  keyPassword?: string;
  keyId?: string;
  keyContent?: string;
  customClaims?: { name: string; value: string }[];
};

export const AUTHENTICATION_TYPES: { label: string; value: WebhookSubscriptionConfigurationAuthType }[] = [
  {
    label: 'No security',
    value: 'none',
  },
  {
    label: 'Basic security',
    value: 'basic',
  },
  {
    label: 'Token security',
    value: 'token',
  },
  {
    label: 'Oauth2 security',
    value: 'oauth2',
  },
  {
    label: 'JWT Profile for OAuth2',
    value: 'jwtProfileOauth2',
  },
];
