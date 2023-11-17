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
import { GroupMapping, IdentityProviderType, RoleMapping } from '../identity-provider';

export interface SocialIdentityProvider {
  id: string;
  name: string;
  description?: string;
  clientId?: string;
  clientSecret?: string;
  scopeDelimiter?: string;
  authorizationHeader?: string;
  type?: IdentityProviderType;
  authorizationEndpoint?: string;
  tokenIntrospectionEndpoint?: string;
  tokenEndpoint?: string;
  userInfoEndpoint?: string;
  userLogoutEndpoint?: string;
  requiredUrlParams?: string[];
  optionalUrlParams?: string[];
  scopes?: string[];
  display?: string;
  color?: string;
  userProfileMapping?: { [key: string]: string };
  groupMappings?: GroupMapping[];
  roleMappings?: RoleMapping[];
  emailRequired?: boolean;
  syncMappings?: boolean;
}
