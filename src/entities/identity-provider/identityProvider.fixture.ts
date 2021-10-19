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
import { GroupMapping, IdentityProvider, RoleMapping } from './identityProvider';

export function fakeGroupMapping(attributes?: Partial<GroupMapping>): GroupMapping {
  const base: GroupMapping = {};

  return {
    ...base,
    ...attributes,
  };
}

export function fakeRoleMapping(attributes?: Partial<RoleMapping>): RoleMapping {
  const base: RoleMapping = {};

  return {
    ...base,
    ...attributes,
  };
}

export function fakeIdentityProvider(attributes?: Partial<IdentityProvider>): IdentityProvider {
  const base: IdentityProvider = {
    id: 'google-idp',
    name: 'Google IDP',
    type: 'GOOGLE',
    description: '',
    enabled: true,
    configuration: {
      clientId: 'Client Id',
      clientSecret: 'Client Secret',
    },
    groupMappings: [],
    roleMappings: [],
    userProfileMapping: { id: '', firstname: '', lastname: '', email: '', picture: '' },
    emailRequired: true,
    syncMappings: false,
  };

  return {
    ...base,
    ...attributes,
  };
}
