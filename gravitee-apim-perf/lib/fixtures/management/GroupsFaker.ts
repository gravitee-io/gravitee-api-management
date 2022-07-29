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
import faker from '@faker-js/faker';
import { NewGroupEntity } from '@management-models/NewGroupEntity';

export class GroupsFaker {
  static newGroup(attributes?: Partial<NewGroupEntity>): NewGroupEntity {
    const name = faker.commerce.productName();

    return {
      name,
      event_rules: [],
      lock_api_role: false,
      lock_application_role: false,
      disable_membership_notifications: false,
      ...attributes,
    };
  }
}
