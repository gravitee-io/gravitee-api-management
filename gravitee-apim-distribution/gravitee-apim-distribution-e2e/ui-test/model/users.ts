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
export interface BasicAuthentication {
  username: string;
  password: string;
}

export interface User {
  displayName: string;
  reference: string;
  id: string;
}

export interface ApiUser {
  firstname: string;
  lastname: string;
  email: string;
  source: string;
  sourceId: string;
  service: boolean;
}

export interface Task {
  type: TaskType;
  data: TaskQuality | object;
}

export enum TaskType {
  SUBSCRIPTION_APPROVAL = 'SUBSCRIPTION_APPROVAL',
  IN_REVIEW = 'IN_REVIEW',
  REQUEST_FOR_CHANGES = 'REQUEST_FOR_CHANGES',
  USER_REGISTRATION_APPROVAL = 'USER_REGISTRATION_APPROVAL',
  PROMOTION_APPROVAL = 'PROMOTION_APPROVAL',
}

export interface TaskQuality {
  id: string;
  referenceType: string;
  referenceId: string;
  type: string;
  state: string;
  user: string;
}
