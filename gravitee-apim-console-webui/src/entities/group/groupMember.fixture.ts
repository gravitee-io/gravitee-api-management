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
import { GroupMember, GroupMembership } from './groupMember';

export function fakeGroupMembership(attributes?: Partial<GroupMembership>): GroupMembership {
  const defaultValue: GroupMembership = {
    id: 'e3dcbf7d-19a4-4470-9cbf-7d19a49470dd',
    roles: [
      { scope: 'GROUP', name: 'ADMIN' },
      { scope: 'API', name: 'USER' },
      { scope: 'APPLICATION', name: 'USER' },
    ],
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}

export function fakeGroupMember(attributes?: Partial<GroupMember>): GroupMember {
  const defaultValue: GroupMember = {
    id: '3ebc7aab-2639-4cab-bc7a-ab2639acab8b',
    displayName: 'Joe Bar',
    roles: {
      APPLICATION: 'USER',
      API: 'USER',
    },
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
