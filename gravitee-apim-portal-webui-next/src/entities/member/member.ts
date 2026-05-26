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
import { User } from '../user/user';

export interface Member {
  id?: string;
  user?: User;
  created_at?: string;
  updated_at?: string;
  role?: string;
}

export interface MemberInput {
  user?: string;
  reference?: string;
  role: string;
}

export interface TransferOwnershipInput {
  new_primary_owner_id: string;
  new_primary_owner_reference?: string;
  primary_owner_newrole: string;
}

export interface MembersResponse {
  data?: Member[];
  metadata?: {
    pagination?: {
      current_page?: number;
      total?: number;
      size?: number;
    };
  };
  links?: unknown;
}

/** Filters supported by member search APIs (e.g. application members `_search`). */
export interface MemberSearchFilters {
  displayName?: string;
}
