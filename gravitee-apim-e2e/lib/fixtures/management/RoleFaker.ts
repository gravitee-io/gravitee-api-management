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
import { faker } from '@faker-js/faker';
import { NewRoleEntity } from '@gravitee/management-webclient-sdk/src/lib/models/NewRoleEntity';
import { RoleScope } from '@gravitee/management-webclient-sdk/src/lib/models/RoleScope';

export class RoleFaker {
  static newRoleEntity(attributes?: Partial<NewRoleEntity>): NewRoleEntity {
    return {
      _default: false,
      description: faker.lorem.words(10),
      name: faker.commerce.productName(),
      permissions: {},
      scope: RoleScope.API,
      ...attributes,
    };
  }
}
