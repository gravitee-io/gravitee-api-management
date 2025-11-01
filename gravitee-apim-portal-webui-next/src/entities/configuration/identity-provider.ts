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

import { IdentityProviderType } from './identity-provider-type';

export interface IdentityProvider {
  // Common props of all identity providers
  id?: string;
  name?: string;
  description?: string;
  client_id?: string;
  email_required?: boolean;
  type?: IdentityProviderType;
  authorizationEndpoint?: string;
  scopes?: Array<string>;

  // Gravitee.io AM and OpenId Connect only
  tokenIntrospectionEndpoint?: string;
  userLogoutEndpoint?: string;
  color?: string;

  // Google only
  display?: string;
  requiredUrlParams?: Array<string>;

  // GitHub and Google only
  optionalUrlParams?: Array<string>;
}
