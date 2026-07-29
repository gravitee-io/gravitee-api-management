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
import { ErrorableManagement, RequestInfo } from '@model/technical';
import Chainable = Cypress.Chainable;
import Response = Cypress.Response;
import { HttpConnector } from '@model/technical.http';
import { QualityRule } from '@model/quality-rules';
import { Group } from '@model/groups';
import { ParamMap } from '@commands/common/http.commands';
import { Member } from '@model/members';

export class EnvironmentConfigurationManagementCommands extends HttpConnector {
  constructor(requestInfo: RequestInfo) {
    super(requestInfo);
  }

  addQualityRule<T extends ErrorableManagement<QualityRule> = QualityRule>(qualityRule: QualityRule): Chainable<Response<T>> {
    return this.httpClient.post('/configuration/quality-rules', qualityRule);
  }

  getQualityRule<T extends ErrorableManagement<QualityRule[]> = QualityRule[]>(): Chainable<Response<T>> {
    return this.httpClient.get('/configuration/quality-rules');
  }

  deleteQualityRule<T extends ErrorableManagement<QualityRule> = QualityRule>(ruleId: string): Chainable<Response<T>> {
    return this.httpClient.delete(`/configuration/quality-rules/${ruleId}`);
  }

  createGroup<T extends ErrorableManagement<Group> = Group>(group: Group): Chainable<Response<T>> {
    return this.httpClient.post('/configuration/groups', group);
  }

  deleteGroup<T extends ErrorableManagement<Group> = Group>(groupId: string): Chainable<Response<T>> {
    return this.httpClient.delete(`/configuration/groups/${groupId}`);
  }

  associateGroup<T extends ErrorableManagement<Group> = Group>(groupId: string, type: ParamMap): Chainable<Response<T>> {
    return this.httpClient.postWithoutBody(`/configuration/groups/${groupId}/memberships`, type);
  }

  addMembersToGroup<T extends ErrorableManagement<Member> = Member>(groupId: string, members: Member[]): Chainable<Response<T>> {
    return this.httpClient.post(`/configuration/groups/${groupId}/members`, members);
  }
}
