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
export class Group {
  id?: string;
  name: string;
  manageable?: boolean;
  roles?: object;
  event_rules: EventRule[];
  lock_api_role: boolean;
  lock_application_role: boolean;
  disable_membership_notifications: boolean;
  created_at?: number;
  updated_at?: number;
  system_invitation?: boolean;
  email_invitation?: boolean;
  primary_owner?: boolean;
}

export class EventRule {
  event: GroupEvent;
}

export enum GroupEvent {
  API_CREATE = 'API_CREATE',
  APPLICATION_CREATE = 'APPLICATION_CREATE',
}
