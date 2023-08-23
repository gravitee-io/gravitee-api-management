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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

export class ApiRuntimeLogsSettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-settings';

  private getEnableToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  public getLogsBanner = this.locatorFor(DivHarness.with({ text: /Logging smartly/ }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Save Settings"]' }));
  private getEntrypointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="request"]' }));
  private getResponseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="response"]' }));
  private getMessageContentCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageContent"]' }));
  private getMessageHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageHeaders"]' }));
  private getMessageMetadataCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageMetadata"]' }));

  public areLogsEnabled = async (): Promise<boolean> => {
    return await this.getEnableToggle().then((toggles) => toggles.isChecked());
  };

  public disableLogs = async (): Promise<void> => {
    return await this.getEnableToggle().then((toggles) => toggles.uncheck());
  };

  public saveSettings = async (): Promise<void> => {
    return this.getSaveButton().then((saveButton) => saveButton.click());
  };

  public isEntrypointChecked = async (): Promise<boolean> => {
    return await this.getEntrypointCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkEntrypoint = async (): Promise<void> => {
    return await this.getEntrypointCheckbox().then((checkbox) => checkbox.check());
  };

  public isEndpointChecked = async (): Promise<boolean> => {
    return await this.getEndpointCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkEndpoint = async (): Promise<void> => {
    return await this.getEndpointCheckbox().then((checkbox) => checkbox.check());
  };

  public isRequestChecked = async (): Promise<boolean> => {
    return await this.getRequestCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkRequest = async (): Promise<void> => {
    return await this.getRequestCheckbox().then((checkbox) => checkbox.check());
  };

  public isResponseChecked = async (): Promise<boolean> => {
    return await this.getResponseCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkResponse = async (): Promise<void> => {
    return await this.getResponseCheckbox().then((checkbox) => checkbox.check());
  };

  public isMessageContentChecked = async (): Promise<boolean> => {
    return await this.getMessageContentCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkMessageContent = async (): Promise<void> => {
    return await this.getMessageContentCheckbox().then((checkbox) => checkbox.check());
  };

  public isMessageHeadersChecked = async (): Promise<boolean> => {
    return await this.getMessageHeadersCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkMessageHeaders = async (): Promise<void> => {
    return await this.getMessageHeadersCheckbox().then((checkbox) => checkbox.check());
  };

  public isMessageMetadataChecked = async (): Promise<boolean> => {
    return await this.getMessageMetadataCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkMessageMetadata = async (): Promise<void> => {
    return await this.getMessageMetadataCheckbox().then((checkbox) => checkbox.check());
  };
}
