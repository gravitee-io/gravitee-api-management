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
import { Event } from './event';

export function fakeEvent(attributes?: Partial<Event>): Event {
  const defaultValue: Event = {
    id: 'event#1',
    parentId: '',
    payload: 'A payload',
    properties: {},
    type: 'DEBUG_API',
    environments: ['environment#1'],
    user: undefined,
    created_at: new Date(),
    updated_at: new Date(),
  };

  return {
    ...defaultValue,
    ...attributes,
  };
}
