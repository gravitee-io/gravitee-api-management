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
import { isFunction } from 'lodash';

import { BaseGroup, Group } from './group';
import { GroupsResponse } from './groupsResponse';

export function fakeBaseGroup(modifier?: Partial<BaseGroup>): BaseGroup {
  const base: BaseGroup = {
    id: '45ff00ef-8256-3218-bf0d-b289735d84bb',
    name: 'group-name',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeGroup(modifier?: Partial<Group>): Group {
  const base: Group = {
    ...fakeBaseGroup(),
    disableMembershipNotifications: true,
    emailInvitation: true,
    systemInvitation: true,
    apiRole: 'USER',
    lockApiRole: true,
    applicationRole: 'READ_ONLY',
    lockApplicationRole: true,
    primaryOwner: true,
    maxInvitation: true,
    manageable: true,
    eventRules: ['APPLICATION_CREATE'],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeGroupsResponse(modifier?: Partial<GroupsResponse>): GroupsResponse {
  const base: GroupsResponse = {
    data: [fakeBaseGroup()],
    pagination: {
      page: 1,
      perPage: 10,
    },
    links: undefined,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
