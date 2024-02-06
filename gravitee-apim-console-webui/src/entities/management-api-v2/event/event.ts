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

import { BaseUser } from '../user';

export type EventType =
  | 'ALERT_NOTIFICATION'
  | 'DEBUG_API'
  | 'GATEWAY_STARTED'
  | 'GATEWAY_STOPPED'
  | 'PUBLISH_API'
  | 'PUBLISH_API_RESULT'
  | 'PUBLISH_DICTIONARY'
  | 'PUBLISH_ORGANIZATION'
  | 'PUBLISH_ORGANIZATION_LICENSE'
  | 'START_API'
  | 'START_DICTIONARY'
  | 'STOP_API'
  | 'STOP_DICTIONARY'
  | 'UNPUBLISH_API'
  | 'UNPUBLISH_API_RESULT'
  | 'UNPUBLISH_DICTIONARY';

export interface Event {
  id: string;
  type: EventType;
  payload: string;
  parentId?: string;
  environmentIds: string[];
  createdAt: Date;
  initiator: BaseUser;
  properties: Record<string, string>;
}

export interface SearchApiEventParam {
  page?: number;
  perPage?: number;
  from?: number;
  to?: number;
  types?: string;
}
