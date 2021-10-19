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
import { Group } from './group';

export function fakeGroup(attributes?: Partial<Group>): Group {
  const defaultValue: Group = {
    id: 'f1194262-9157-4986-9942-629157f98682',
    name: 'Group 1',
    manageable: true,
    roles: {},
    created_at: 1632300592527,
    updated_at: 1632300592527,
    max_invitation: 12,
    lock_api_role: false,
    lock_application_role: false,
    system_invitation: false,
    email_invitation: true,
    disable_membership_notifications: false,
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
