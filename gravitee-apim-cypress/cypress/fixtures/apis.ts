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
import {testUrls} from "./urls";
import {User} from "./users";

const { managementApi } = testUrls;

export interface Api {
  contextPath: string;
  description: string;
  endpoint: string;
  name: string;
  version: string;
}

export class Apis {

  static create(auth: User, api: Api) {

    return cy.request({
      method: "POST",
      url: `${managementApi}/apis`,
      auth,
      body: api,
    });

  }

  static start(auth: User, apiId: string) {

    return cy.request({
      method: "POST",
      url: `${managementApi}/apis/${apiId}?action=START`,
      auth
    });

  }

  static publish(auth: User, apiId: string, api: any) {

    const body = {...api,  lifecycle_state: 'PUBLISHED', visibility: 'PUBLIC' };
    delete body.id;
    delete body.state;
    delete body.created_id;
    delete body.updated_at;
    delete body.owner;
    delete body.context_path;

    return cy.request({
      method: "PUT",
      url: `${managementApi}/apis/${apiId}`,
      auth,
      body,
    });

  }
}
