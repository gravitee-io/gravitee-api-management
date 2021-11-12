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
import { MembershipListItem } from './membershipListItem';

export function fakeMembershipListItem(attributes?: Partial<MembershipListItem>): MembershipListItem {
  const base: MembershipListItem = {
    id: 'eb453792-dbfc-4e10-8537-92dbfc2e10c4',
    displayName: 'Gaetan Maisse',
    role: 'USER',
  };

  return {
    ...base,
    ...attributes,
  };
}
