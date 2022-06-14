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
export interface GroupMapping {
  condition?: string;
  groups?: string[];
}

export interface RoleMapping {
  condition?: string;
  environments?: Record<string, string[]>;
  organizations?: string[];
}

export type IdentityProviderType = 'GOOGLE' | 'GITHUB' | 'GRAVITEEIO_AM' | 'OIDC';

export interface IdentityProviderGoogleConfiguration {
  clientId?: string;
  clientSecret?: string;
}
export interface IdentityProviderGithubConfiguration {
  clientId?: string;
  clientSecret?: string;
}

export interface IdentityProviderGraviteeioAmConfiguration {
  clientId?: string;
  clientSecret?: string;
  color?: string;
  domain?: string;
  scopes?: string[];
  serverURL?: string;
}

export interface IdentityProviderOidcConfiguration {
  clientId?: string;
  clientSecret?: string;
  authorizeEndpoint?: string;
  color?: string;
  scopes?: string[];
  tokenEndpoint?: string;
  tokenIntrospectionEndpoint?: string;
  userInfoEndpoint?: string;
  userLogoutEndpoint?: string;
}

export type IdentityProviderConfiguration =
  | IdentityProviderGoogleConfiguration
  | IdentityProviderGithubConfiguration
  | IdentityProviderGraviteeioAmConfiguration
  | IdentityProviderOidcConfiguration;

export interface IdentityProviderUserProfileMapping {
  id: string;
  firstname?: string;
  lastname?: string;
  email?: string;
  picture?: string;
}

export interface IdentityProvider {
  id?: string;
  name?: string;
  description?: string;
  type?: IdentityProviderType;
  enabled?: boolean;
  configuration?: IdentityProviderConfiguration;
  groupMappings?: GroupMapping[];
  roleMappings?: RoleMapping[];
  userProfileMapping?: IdentityProviderUserProfileMapping;
  emailRequired?: boolean;
  syncMappings?: boolean;
  requiredUrlParams?: string[];
  optionalUrlParams?: string[];
  // @deprecated
  scopes?: any;
  // @deprecated
  scope?: any;
  // @deprecated
  userLogoutEndpoint?: any;
}
