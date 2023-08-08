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

export interface BaseGroup {
  id?: string;
  name?: string;
}

export type GroupEvent = 'API_CREATE' | 'APPLICATION_CREATE' | undefined;

export interface Group extends BaseGroup {
  eventRules?: GroupEvent[];
  manageable?: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  maxInvitation?: boolean;
  apiRole?: string;
  lockApiRole?: boolean;
  applicationRole?: string;
  lockApplicationRole?: boolean;
  systemInvitation?: boolean;
  emailInvitation?: boolean;
  disableMembershipNotifications?: boolean;
  primaryOwner?: boolean;
}
