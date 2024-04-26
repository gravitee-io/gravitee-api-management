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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { User } from './user';

export function fakeUser(modifier?: Partial<User> | ((baseApi: User) => User)): User {
  const base: User = {
    id: '8d4ce9b8-0efe-4d8b-8ce9-b80efe1d8bf1',
    email: 'gaetan.maisse@graviteesource.com',
    first_name: 'Hello',
    last_name: 'World',
    config: {},
    reference: 'user-reference',
    permissions: {
      USER: [],
      APPLICATION: [],
    },
    customFields: {},
    display_name: 'admin',
    editable_profile: false,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
