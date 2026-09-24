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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class PortalSettingsPageHarness extends ComponentHarness {
  static readonly hostSelector = 'portal-settings-page';

  private readonly apiKeyHeaderInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="apikeyHeader"]' }));
  private readonly portalUrlInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="url"]' }));
  private readonly kafkaSaslMechanismsSelect = this.locatorFor(
    MatSelectHarness.with({ selector: '[data-testid="kafka-sasl-mechanisms"]' }),
  );
  private readonly registrationToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="user-registration-toggle"]' }),
  );
  private readonly automaticValidationToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="automatic-validation-toggle"]' }),
  );
  private readonly swaggerViewer = this.locatorFor(MatRadioButtonHarness.with({ selector: '[data-testid="swagger-viewer"]' }));
  private readonly redocViewer = this.locatorFor(MatRadioButtonHarness.with({ selector: '[data-testid="redoc-viewer"]' }));
  private readonly portalCapabilitiesCard = this.locatorForOptional('[data-testid="portal-capabilities-card"]');
  private readonly mtlsToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid="portal-next-mtls-toggle"]' }));
  private readonly analyticsToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="portal-next-analytics-toggle"]' }),
  );
  private readonly fuzzySearchToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="portal-next-fuzzy-search-toggle"]' }),
  );
  private readonly applicationMembershipCard = this.locatorForOptional('[data-testid="application-membership-card"]');
  private readonly portalNextDisabledBanner = this.locatorForOptional('[data-testid="portal-next-disabled-banner"]');
  private readonly applicationMembershipToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="application-membership-toggle"]' }),
  );
  private readonly transferOwnershipToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="transfer-ownership-toggle"]' }),
  );
  private readonly membershipInvitationsToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="membership-invitations-toggle"]' }),
  );
  private readonly saveBar = this.locatorFor(GioSaveBarHarness);
  private readonly errorBanner = this.locatorForOptional('[data-testid="settings-load-error"]');
  private readonly retryButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="settings-retry"]' }));

  async getApiKeyHeader(): Promise<string> {
    return (await this.apiKeyHeaderInput()).getValue();
  }

  async setApiKeyHeader(value: string): Promise<void> {
    await (await this.apiKeyHeaderInput()).setValue(value);
  }

  async isApiKeyHeaderDisabled(): Promise<boolean> {
    return (await this.apiKeyHeaderInput()).isDisabled();
  }

  async getPortalUrl(): Promise<string> {
    return (await this.portalUrlInput()).getValue();
  }

  async isPortalUrlDisabled(): Promise<boolean> {
    return (await this.portalUrlInput()).isDisabled();
  }

  async getKafkaSaslMechanisms(): Promise<string> {
    return (await this.kafkaSaslMechanismsSelect()).getValueText();
  }

  async toggleKafkaSaslMechanism(mechanism: string): Promise<void> {
    const select = await this.kafkaSaslMechanismsSelect();
    await select.open();
    await select.clickOptions({ text: mechanism });
    await select.close();
  }

  async isKafkaSaslMechanismsDisabled(): Promise<boolean> {
    return (await this.kafkaSaslMechanismsSelect()).isDisabled();
  }

  async getRegistrationToggle(): Promise<MatSlideToggleHarness> {
    return this.registrationToggle();
  }

  async getAutomaticValidationToggle(): Promise<MatSlideToggleHarness> {
    return this.automaticValidationToggle();
  }

  async getSwaggerViewer(): Promise<MatRadioButtonHarness> {
    return this.swaggerViewer();
  }

  async getRedocViewer(): Promise<MatRadioButtonHarness> {
    return this.redocViewer();
  }

  async hasPortalCapabilitiesCard(): Promise<boolean> {
    return (await this.portalCapabilitiesCard()) !== null;
  }

  async getMtlsToggle(): Promise<MatSlideToggleHarness> {
    return this.mtlsToggle();
  }

  async getAnalyticsToggle(): Promise<MatSlideToggleHarness> {
    return this.analyticsToggle();
  }

  async getFuzzySearchToggle(): Promise<MatSlideToggleHarness> {
    return this.fuzzySearchToggle();
  }

  async hasApplicationMembershipCard(): Promise<boolean> {
    return (await this.applicationMembershipCard()) !== null;
  }

  async hasPortalNextDisabledBanner(): Promise<boolean> {
    return (await this.portalNextDisabledBanner()) !== null;
  }

  async getApplicationMembershipToggle(): Promise<MatSlideToggleHarness> {
    return this.applicationMembershipToggle();
  }

  async getTransferOwnershipToggle(): Promise<MatSlideToggleHarness> {
    return this.transferOwnershipToggle();
  }

  async getMembershipInvitationsToggle(): Promise<MatSlideToggleHarness> {
    return this.membershipInvitationsToggle();
  }

  async submit(): Promise<void> {
    await (await this.saveBar()).clickSubmit();
  }

  async reset(): Promise<void> {
    await (await this.saveBar()).clickReset();
  }

  async hasLoadError(): Promise<boolean> {
    return (await this.errorBanner()) !== null;
  }

  async retry(): Promise<void> {
    await (await this.retryButton()).click();
  }
}
