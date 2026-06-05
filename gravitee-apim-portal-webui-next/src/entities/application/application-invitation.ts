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
export interface ApplicationInvitation {
  id: string;
  email: string;
  role: string;
}

export interface ApplicationInvitationRecipientInput {
  email: string;
}

export interface ApplicationInvitationsCreateInput {
  recipients: ApplicationInvitationRecipientInput[];
  role: string;
  notify: boolean;
  confirmation_page_url?: string;
}

export interface ApplicationInvitationUpdateInput {
  role: string;
}

export interface ApplicationInvitationsSearchFilters {
  email?: string;
}

export interface ApplicationInvitationsResponse {
  data?: ApplicationInvitation[];
  metadata?: {
    paginateMetaData?: {
      totalElements?: number;
    };
    pagination?: {
      current_page?: number;
      size?: number;
      total?: number;
    };
  };
  links?: unknown;
}
