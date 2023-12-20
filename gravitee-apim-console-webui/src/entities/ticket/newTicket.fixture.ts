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
import { NewTicket } from './newTicket';

export function fakeNewTicket(attributes?: Partial<NewTicket>): NewTicket {
  const base: NewTicket = {
    subject: 'A simple ticket',
    content: `Aper etiam atque etiam has exponere, ut quaeque`,
    api: '08cbff9a-1b1f-4430-836d-8849b0b23e7a',
    application: 'da428f6f-b986-4a70-983b-55b83a9e7587',
    copyToSender: true,
  };

  return {
    ...base,
    ...attributes,
  };
}
