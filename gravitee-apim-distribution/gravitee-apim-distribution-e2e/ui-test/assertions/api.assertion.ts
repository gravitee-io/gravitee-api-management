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
import { Api, ApiLifecycleState, ApiQualityMetrics, ApiState, ApiVisibility, ApiWorkflowState, PortalApi } from '@model/apis';

export class ApiAssertions {
  private apiResponse: Cypress.Response<Api>;
  private api: Api;

  private constructor(apiResponse: Cypress.Response<Api>) {
    this.apiResponse = apiResponse;
    this.api = apiResponse.body;
  }

  static assertThat(apiResponse: Cypress.Response<Api>) {
    return new ApiAssertions(apiResponse);
  }

  hasBeenCreated(expected: Api) {
    this.hasId()
      .hasState(ApiState.STOPPED)
      .hasVisibility(ApiVisibility.PRIVATE)
      .hasLifecycleState(ApiLifecycleState.CREATED)
      .hasName(expected.name)
      .hasDescription(expected.description);
    return this;
  }

  hasBeenPublished(expected: Api) {
    this.hasId()
      .hasState(ApiState.STOPPED)
      .hasVisibility(ApiVisibility.PRIVATE)
      .hasLifecycleState(ApiLifecycleState.PUBLISHED)
      .hasName(expected.name)
      .hasDescription(expected.description);
    return this;
  }

  hasId(expectedId?: string) {
    if (expectedId) {
      expect(this.api.id, 'has expected id').to.be.equal(expectedId);
    } else {
      expect(this.api.id, 'has an id').not.undefined;
    }
    return this;
  }

  hasName(expectedName: string) {
    expect(this.api.name, 'has expected name').to.be.equal(expectedName);
    return this;
  }

  hasDescription(expectedDescription: string) {
    expect(this.api.description, 'has expected description').to.be.equal(expectedDescription);
    return this;
  }

  hasVisibility(expectedVisibility: ApiVisibility) {
    expect(this.api.visibility, 'has expected visibility').to.eq(expectedVisibility);
    return this;
  }

  hasState(expectedState: ApiState) {
    expect(this.api.state, 'has expected state').to.eq(expectedState);
    return this;
  }

  hasLifecycleState(expectedLifecycleState: ApiLifecycleState) {
    expect(this.api.lifecycle_state, 'has expected lifecycle state').to.eq(expectedLifecycleState);
    return this;
  }

  hasWorkflowState(expectedWorkflowState: ApiWorkflowState) {
    expect(this.api.workflow_state, 'has expected workflow state').to.eq(expectedWorkflowState);
    return this;
  }

  hasLabels() {
    expect(this.api.labels, 'has labels').to.not.be.empty;
    return this;
  }

  containsLabel(expectedLabel: string) {
    expect(this.api.labels, 'contains label').to.contains(expectedLabel);
    return this;
  }
}

export class PortalApiAssertions {
  private apiResponse: Cypress.Response<PortalApi>;
  private api: PortalApi;

  private constructor(apiResponse: Cypress.Response<PortalApi>) {
    this.apiResponse = apiResponse;
    this.api = apiResponse.body;
  }

  static assertThat(apiResponse: Cypress.Response<PortalApi>) {
    return new PortalApiAssertions(apiResponse);
  }

  hasId(expectedId: string) {
    expect(this.api.id, 'has expected id').to.be.equal(expectedId);
    return this;
  }

  isNotRunning() {
    expect(this.api.running, 'is not running').to.be.false;
    return this;
  }

  isRunning() {
    expect(this.api.running, 'is running').to.be.true;
    return this;
  }

  isNotPublic() {
    expect(this.api.public, 'is not public').to.be.false;
    return this;
  }

  isPublic() {
    expect(this.api.public, 'is public').to.be.true;
    return this;
  }

  isNotDraft() {
    expect(this.api.draft, 'is not draft').to.be.false;
    return this;
  }

  isDraft() {
    expect(this.api.draft, 'is draft').to.be.true;
    return this;
  }
}

export class ApiQualityMetricsAssertions {
  private metricsResponse: Cypress.Response<ApiQualityMetrics>;
  private metrics: ApiQualityMetrics;

  private constructor(metricsResponse: Cypress.Response<ApiQualityMetrics>) {
    this.metricsResponse = metricsResponse;
    this.metrics = metricsResponse.body;
  }

  static assertThat(metricsResponse: Cypress.Response<ApiQualityMetrics>) {
    return new ApiQualityMetricsAssertions(metricsResponse);
  }

  hasScore(expectedScore: number) {
    expect(this.metrics.score).to.eq(expectedScore);
    return this;
  }

  hasMetric(metricKey: string, metricValue: boolean) {
    expect(metricKey in this.metrics.metrics_passed, `has metrics key: ${metricKey}`).to.be.true;
    expect(this.metrics.metrics_passed[metricKey], `has metrics value: ${metricKey}`).to.eq(metricValue);
    return this;
  }
}
