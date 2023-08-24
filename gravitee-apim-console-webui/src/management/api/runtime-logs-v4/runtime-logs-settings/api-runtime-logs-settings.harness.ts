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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';

export class ApiRuntimeLogsSettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-settings';

  private getEnableToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  public getLogsBanner = this.locatorFor(DivHarness.with({ text: /Logging smartly/ }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Save Settings"]' }));
  private getEntrypointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestPhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="request"]' }));
  private getResponsePhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="response"]' }));
  private getMessageContentCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageContent"]' }));
  private getMessageHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageHeaders"]' }));
  private getMessageMetadataCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageMetadata"]' }));
  private getRequestPayloadCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="requestPayload"]' }));
  private getRequestHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="requestHeaders"]' }));
  private getMessageConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="messageCondition"]' }));
  private getRequestConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="requestCondition"]' }));
  private getSamplingTypeToggle = this.locatorFor(MatButtonToggleGroupHarness.with({ selector: '[formControlName="samplingType"]' }));
  private getSamplingValueInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="samplingValue"]' }));

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

  public isRequestPhaseChecked = async (): Promise<boolean> => {
    return await this.getRequestPhaseCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkRequestPhase = async (): Promise<void> => {
    return await this.getRequestPhaseCheckbox().then((checkbox) => checkbox.check());
  };

  public isResponsePhaseChecked = async (): Promise<boolean> => {
    return await this.getResponsePhaseCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkResponsePhase = async (): Promise<void> => {
    return await this.getResponsePhaseCheckbox().then((checkbox) => checkbox.check());
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

  public isRequestPayloadChecked = async (): Promise<boolean> => {
    return await this.getRequestPayloadCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkRequestPayload = async (): Promise<void> => {
    return await this.getRequestPayloadCheckbox().then((checkbox) => checkbox.check());
  };

  public isRequestHeadersChecked = async (): Promise<boolean> => {
    return await this.getRequestHeadersCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkRequestHeaders = async (): Promise<void> => {
    return await this.getRequestHeadersCheckbox().then((checkbox) => checkbox.check());
  };

  public getMessageCondition = async (): Promise<string> => {
    return await this.getMessageConditionInput().then((input) => input.getValue());
  };

  public addMessageCondition = async (condition: string): Promise<void> => {
    return await this.getMessageConditionInput().then((input) => input.setValue(condition));
  };

  public getRequestCondition = async (): Promise<string> => {
    return await this.getRequestConditionInput().then((input) => input.getValue());
  };

  public addRequestCondition = async (condition: string): Promise<void> => {
    return await this.getRequestConditionInput().then((input) => input.setValue(condition));
  };

  public choseSamplingType(type: string): Promise<void> {
    return this.getSamplingTypeToggle()
      .then((group) => group.getToggles({ text: type }))
      .then((toggles) => toggles[0].check());
  }

  public getSamplingType = async (): Promise<string> => {
    return await this.getSamplingTypeToggle()
      .then((group) => group.getToggles())
      .then(async (toggles) => {
        for (const toggle of toggles) {
          if (await toggle.isChecked()) {
            return toggle.getText();
          }
        }
        return '';
      });
  };

  public addSamplingValue = async (value: string): Promise<void> => {
    return await this.getSamplingValueInput().then((input) => input.setValue(value));
  };

  public getSamplingValue = async (): Promise<string> => {
    return await this.getSamplingValueInput().then((input) => input.getValue());
  };
}
