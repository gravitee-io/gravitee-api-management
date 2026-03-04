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

import { MemberV2, MembersV2Response } from './application-members';

export function fakeMember(modifier?: Partial<MemberV2> | ((base: MemberV2) => MemberV2)): MemberV2 {
  const base: MemberV2 = {
    id: 'member-1',
    display_name: 'Admin master',
    email: 'admin@company.com',
    role: 'PRIMARY_OWNER',
    created_at: '2025-01-15T10:00:00Z',
    updated_at: '2025-01-15T10:00:00Z',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeMembersResponse(
  modifier?: Partial<MembersV2Response> | ((base: MembersV2Response) => MembersV2Response),
): MembersV2Response {
  const base: MembersV2Response = {
    data: [
      fakeMember(),
      fakeMember({ id: 'member-2', display_name: 'Person 3', email: 'person3@company.com', role: 'VIEWER' }),
      fakeMember({ id: 'member-3', display_name: 'Elliot Goldblatt', email: 'elliot.goldblatt@graviteesource.com', role: 'OWNER' }),
    ],
    metadata: {
      pagination: {
        current_page: 1,
        first: 1,
        last: 1,
        size: 3,
        total: 3,
        total_pages: 1,
      },
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
