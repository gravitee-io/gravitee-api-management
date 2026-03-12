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
import { ComponentHarness } from '@angular/cdk/testing';

export class ApplicationTabSettingsReadHarness extends ComponentHarness {
  public static hostSelector = 'app-application-tab-settings-read';

  protected locateName = this.locatorFor('[data-testId="name"]');
  protected locateOwner = this.locatorForOptional('[data-testId="owner"]');
  protected locateType = this.locatorFor('[data-testId="type"]');
  protected locateSecurityType = this.locatorFor('[data-testId="securityType"]');
  protected locateDescription = this.locatorFor('[data-testId="description"]');
  protected locateDomain = this.locatorFor('[data-testId="domain"]');
  protected locateGrantTypes = this.locatorForOptional('[data-testId="grantTypes"]');
  protected locateClientId = this.locatorForOptional('[data-testId="clientId"]');
  protected locateRedirectUris = this.locatorForOptional('[data-testId="redirectUris"]');
  protected locateEditButton = this.locatorForOptional('[data-testId="edit"]');

  public async getName(): Promise<string> {
    return this.locateName().then(el => el.text());
  }

  public async getOwner(): Promise<string | undefined> {
    return this.locateOwner().then(el => el?.text());
  }

  public async getType(): Promise<string> {
    return this.locateType().then(el => el.text());
  }

  public async getSecurityType(): Promise<string> {
    return this.locateSecurityType().then(el => el.text());
  }

  public async getDescription(): Promise<string> {
    return this.locateDescription().then(el => el.text());
  }

  public async getDomain(): Promise<string> {
    return this.locateDomain().then(el => el.text());
  }

  public async getGrantTypes(): Promise<string | undefined> {
    return this.locateGrantTypes().then(el => el?.text());
  }

  public async getClientId(): Promise<string | undefined> {
    return this.locateClientId().then(el => el?.text());
  }

  public async getRedirectUris(): Promise<string | undefined> {
    return this.locateRedirectUris().then(el => el?.text());
  }

  public async canEdit(): Promise<boolean> {
    return this.locateEditButton().then(btn => btn !== null);
  }
}
