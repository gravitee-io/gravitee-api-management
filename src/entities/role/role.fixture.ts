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
import { Role } from './role';

export function fakeRole(attributes?: Partial<Role>): Role {
  const defaultValue: Role = {
    id: '095a7c73-9865-411d-9a7c-739865711d13',
    default: true,
    description: 'Default Organization Role. Created by Gravitee.io.',
    name: 'USER',
    permissions: { ROLE: ['R'], ENVIRONMENT: ['R'], TAG: ['R'], ENTRYPOINT: ['R'], TENANT: ['R'] },
    scope: 'ORGANIZATION',
    system: false,
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
