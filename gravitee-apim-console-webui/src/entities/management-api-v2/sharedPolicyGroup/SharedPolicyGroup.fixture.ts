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

import { SharedPolicyGroup } from './SharedPolicyGroup';

export function fakeSharedPolicyGroup(
  modifier?: Partial<SharedPolicyGroup> | ((base: SharedPolicyGroup) => SharedPolicyGroup),
): SharedPolicyGroup {
  const base: SharedPolicyGroup = {
    id: '1',
    crossId: '1',
    version: 1,
    name: 'Shared policy group',
    phase: 'REQUEST',
    apiType: 'PROXY',
    updatedAt: new Date('2024-04-04T00:00:00Z'),
    deployedAt: new Date('2024-04-04T00:00:00Z'),
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
