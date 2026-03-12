/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ContentContainerComponentHarness } from '@angular/cdk/testing';

import { CopyCodeHarness } from '../../../../../components/copy-code/copy-code.harness';

export class ApplicationTabSettingsReadHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-application-tab-settings-read';
  protected locateCardTitle = this.locatorFor('[data-testId="infoCardTitle"]');
  protected locateAppType = this.locatorFor('[data-testId="type"]');
  protected locateAppTypeDescription = this.locatorFor('[data-testId="typeDescription"]');
  protected locateRedirectUris = this.locatorForOptional('[data-testId="redirectUris"]');
  protected locateGrantTypes = this.locatorForOptional('[data-testId="grantTypes"]');

  public async getInfoCardTitle(): Promise<string> {
    return await this.locateCardTitle().then(cardTitle => cardTitle.text());
  }

  public async getInfoCardApplicationType(): Promise<string> {
    return await this.locateAppType()
      .then(title => title.text())
      .then(text => text.replace(':', ''));
  }

  public async getInfoCardApplicationTypeDescription(): Promise<string> {
    return await this.locateAppTypeDescription().then(description => description.text());
  }

  public async getInfoCardRedirectUris(): Promise<string | undefined> {
    return await this.locateRedirectUris().then(uris => uris?.text());
  }

  public async getInfoCardGrantTypes(): Promise<string | undefined> {
    return await this.locateGrantTypes().then(types => types?.text());
  }

  public async getClientId(): Promise<string | undefined> {
    return await this.getCopyCodeHarnessOrNull('clientId').then(cardCodeHarness => cardCodeHarness?.getText());
  }

  public async getHiddenClientSecret(): Promise<string | undefined> {
    return await this.getCopyCodeHarnessOrNull('clientSecret').then(cardCodeHarness => cardCodeHarness?.getText());
  }

  public async getClearClientSecret(): Promise<string | undefined> {
    const copyCodeHarnessOrNull = this.getCopyCodeHarnessOrNull('clientSecret');
    await copyCodeHarnessOrNull.then(cardCodeHarness => cardCodeHarness?.changePasswordVisibility());
    return copyCodeHarnessOrNull?.then(harness => harness?.getText());
  }

  private async getCopyCodeHarnessOrNull(title: string): Promise<CopyCodeHarness | null> {
    return await this.getHarnessOrNull(CopyCodeHarness.with({ selector: `[data-testId="${title}"]` }));
  }
}
