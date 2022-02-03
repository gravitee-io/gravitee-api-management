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
import { Step } from '@model/plan';

export class PolicyFakers {
  static jwtPolicy(secret: string, attributes?: Partial<Step>): Step {
    return {
      name: 'JSON Web Tokens',
      policy: 'jwt',
      description: 'This policy was created by a Cypress test',
      condition: '',
      enabled: true,
      configuration: {
        propagateAuthHeader: true,
        signature: 'HMAC_HS256',
        resolverParameter: secret,
        publicKeyResolver: 'GIVEN_KEY',
        useSystemProxy: false,
        extractClaims: false,
        userClaim: 'sub',
      },
      ...attributes,
    };
  }

  static oauth2Policy(oauthResource: string, attributes?: Partial<Step>): Step {
    return Cypress._.merge(
      {
        name: 'OAuth2',
        policy: 'oauth2',
        description: 'This policy was created by a Cypress test',
        condition: '',
        enabled: true,
        configuration: {
          extractPayload: false,
          checkRequiredScopes: false,
          modeStrict: true,
          propagateAuthHeader: true,
          oauthResource,
        },
      },
      attributes,
    );
  }
}
