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
import { NewPreRegisterUser } from './newPreRegisterUser';

export function fakeNewPreregisterUser(attributes?: Partial<NewPreRegisterUser>): NewPreRegisterUser {
  const base: NewPreRegisterUser = {
    firstname: 'Bruce',
    lastname: 'Wayne',
    email: 'me@batman.com',
    source: 'memory',
    picture: 'https://batman.com/photo.jpeg',
    sourceId: 'batman',
    newsletter: true,
    customFields: {},
  };
  return {
    ...base,
    ...attributes,
  };
}
