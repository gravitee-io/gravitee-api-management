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
import { isFunction } from 'lodash';

import { ApplicationInvitation, ApplicationInvitationsResponse } from './application-invitation';

export function fakeApplicationInvitation(
  modifier?: Partial<ApplicationInvitation> | ((base: ApplicationInvitation) => ApplicationInvitation),
): ApplicationInvitation {
  const base: ApplicationInvitation = {
    id: 'invitation-id',
    email: 'alice@example.com',
    role: 'USER',
  };
  return isFunction(modifier) ? modifier(base) : { ...base, ...modifier };
}

export function fakeApplicationInvitationsResponse(
  invitations: ApplicationInvitation[] = [fakeApplicationInvitation()],
  total = invitations.length,
): ApplicationInvitationsResponse {
  return {
    data: invitations,
    metadata: {
      paginateMetaData: {
        totalElements: total,
      },
      pagination: {
        current_page: 1,
        size: 10,
        total,
      },
    },
  };
}
