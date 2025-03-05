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
import { NewPreRegisterUserEntity } from '@gravitee/management-webclient-sdk/src/lib/models/NewPreRegisterUserEntity';
import { RegisterUserInput } from '@gravitee/portal-webclient-sdk/src/lib/models/RegisterUserInput';

export class UsersFaker {
  static newNewPreRegisterUserEntity(attributes?: Partial<NewPreRegisterUserEntity>): NewPreRegisterUserEntity {
    const firstName = faker.person.firstName();
    const lastName = faker.person.lastName();
    const email = faker.internet.email({ firstName, lastName });

    return {
      firstname: firstName,
      lastname: lastName,
      email,
      source: 'gravitee',
      sourceId: email,
      service: false,
      ...attributes,
    };
  }

  static newRegisterUserInput(attributes?: Partial<RegisterUserInput>): RegisterUserInput {
    return UsersFaker.newNewPreRegisterUserEntity(attributes);
  }
}
