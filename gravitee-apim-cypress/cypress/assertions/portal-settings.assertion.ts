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
import { PortalSettings } from '@model/portal-settings';

export class PortalSettingsAssertions {
  private settingsResponse: Cypress.Response<PortalSettings>;
  private settings: PortalSettings;

  private constructor(settingsResponse: Cypress.Response<PortalSettings>) {
    this.settingsResponse = settingsResponse;
    this.settings = settingsResponse.body;
  }

  static assertThat(settingsResponse: Cypress.Response<PortalSettings>) {
    return new PortalSettingsAssertions(settingsResponse);
  }

  hasApiReviewEnabled() {
    expect(this.settings.apiReview.enabled, 'has apiReview enabled').to.be.true;
    return this;
  }

  hasApiReviewDisabled() {
    expect(this.settings.apiReview.enabled, 'has apiReview disabled').to.be.false;
    return this;
  }

  hasApiQualityMetricsEnabled() {
    expect(this.settings.apiQualityMetrics.enabled, 'has apiQualityMetrics enabled').to.be.true;
    return this;
  }

  hasApiQualityMetricsDisabled() {
    expect(this.settings.apiQualityMetrics.enabled, 'has apiQualityMetrics disabled').to.be.false;
    return this;
  }
}
