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
import { QualityRule } from '@model/quality-rules';
import { Group } from '@model/groups';

export class EnvironmentQualityRuleAssertions {
  private response: Cypress.Response<QualityRule>;
  private qualityRule: QualityRule;

  private constructor(response: Cypress.Response<QualityRule>) {
    this.response = response;
    this.qualityRule = response.body;
  }

  static assertThat(response: Cypress.Response<QualityRule>) {
    return new EnvironmentQualityRuleAssertions(response);
  }

  hasName(expectedName: string) {
    expect(this.qualityRule.name, 'has expected name').to.be.equal(expectedName);
    return this;
  }

  hasDescription(expectedDescription: string) {
    expect(this.qualityRule.description, 'has expected description').to.be.equal(expectedDescription);
    return this;
  }

  hasWeight(weight: number) {
    expect(this.qualityRule.weight, 'has expected weight').to.be.equal(weight);
    return this;
  }
}

export class EnvironmentGroupAssertions {
  private response: Cypress.Response<Group>;
  private group: Group;

  private constructor(response: Cypress.Response<Group>) {
    this.response = response;
    this.group = response.body;
  }

  static assertThat(response: Cypress.Response<Group>) {
    return new EnvironmentGroupAssertions(response);
  }

  hasName(expectedName: string) {
    expect(this.group.name, 'has expected name').to.be.equal(expectedName);
    return this;
  }
}
