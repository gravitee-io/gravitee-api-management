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
import { BasicAuthentication } from '@model/users';
import { RequestInfo } from '@model/technical';
import { ManagementCommands } from './management.commands';
import { PortalCommands } from './portal.commands';

class GraviteeCommands {
  static management(auth?: BasicAuthentication): ManagementCommands {
    const requestInfo: RequestInfo = {
      auth,
      baseUrl: `${Cypress.env('managementApi')}${Cypress.env('defaultOrgEnv')}`,
    };
    return new ManagementCommands(requestInfo);
  }

  static portal(auth?: BasicAuthentication): PortalCommands {
    const requestInfo: RequestInfo = {
      auth,
      baseUrl: `${Cypress.env('managementApi')}${Cypress.env('portalApi')}`,
    };
    return new PortalCommands(requestInfo);
  }
}
export { GraviteeCommands as gio };
