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
export interface MemberV2 {
  id: string;
  role: string;
  status: string;
  created_at?: string;
  updated_at?: string;
  user: {
    id: string;
    display_name: string;
    email?: string;
  };
}

export interface ApplicationRoleV2 {
  id: string;
  name: string;
  default?: boolean;
  system?: boolean;
}

export interface ApplicationRolesV2Response {
  data: ApplicationRoleV2[];
}

export interface SearchableUser {
  id: string;
  display_name: string;
  email?: string;
}

export interface SearchUsersV2Response {
  data: SearchableUser[];
}

export interface AddMemberInput {
  userId: string;
  role: string;
}

export interface AddMembersRequest {
  members: AddMemberInput[];
  notify: boolean;
}

export interface TransferOwnershipRequest {
  newOwnerId: string;
  newOwnerReference: 'member' | 'user' | 'group';
  previousOwnerNewRole: string;
}

export interface MembersV2Response {
  data: MemberV2[];
  metadata?: {
    pagination?: {
      current_page?: number;
      first?: number;
      last?: number;
      size?: number;
      total?: number;
      total_pages?: number;
    };
  };
}
