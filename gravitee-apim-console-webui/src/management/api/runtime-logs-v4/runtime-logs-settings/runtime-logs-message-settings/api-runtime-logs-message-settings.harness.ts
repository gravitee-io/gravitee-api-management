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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';

export class ApiRuntimeLogsMessageSettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-settings';

  public getLogsBanner = this.locatorFor(DivHarness.with({ text: /Logging smartly/ }));
  private getSaveButton = this.locatorFor(GioSaveBarHarness);
  private getEntrypointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestPhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="request"]' }));
  private getResponsePhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="response"]' }));
  private getMessageContentCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageContent"]' }));
  private getMessageHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageHeaders"]' }));
  private getMessageMetadataCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="messageMetadata"]' }));
  private getHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="headers"]' }));
  private getMessageConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="messageCondition"]' }));
  private getRequestConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="requestCondition"]' }));
  private getSamplingTypeToggle = this.locatorFor(MatButtonToggleGroupHarness.with({ selector: '[formControlName="samplingType"]' }));
  public getSamplingValueInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="samplingValue"]' }));
  private getSamplingValueFormField = this.locatorFor(MatFormFieldHarness.with({ selector: '[data-testid=sampling_value]' }));

  public saveSettings = async (): Promise<void> => {
    return this.getSaveButton().then((saveButton) => saveButton.clickSubmit());
  };

  public isSaveButtonInvalid = async (): Promise<boolean> => {
    return this.getSaveButton().then((saveButton) => saveButton.isSubmitButtonInvalid());
  };

  public resetSettings = async (): Promise<void> => {
    return this.getSaveButton().then((saveButton) => saveButton.clickReset());
  };

  public isEntrypointChecked = async (): Promise<boolean> => {
    return await this.getEntrypointCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public toggleEntrypoint = async (): Promise<void> => {
    return await this.getEntrypointCheckbox().then((checkbox) => checkbox.toggle());
  };

  public isEndpointChecked = async (): Promise<boolean> => {
    return await this.getEndpointCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public toggleEndpoint = async (): Promise<void> => {
    return await this.getEndpointCheckbox().then((checkbox) => checkbox.toggle());
  };

  public isRequestPhaseChecked = async (): Promise<boolean> => {
    return await this.getRequestPhaseCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkRequestPhase = async (): Promise<void> => {
    return await this.getRequestPhaseCheckbox().then((checkbox) => checkbox.check());
  };

  public uncheckRequestPhase = async (): Promise<void> => {
    return await this.getRequestPhaseCheckbox().then((checkbox) => checkbox.uncheck());
  };

  public isResponsePhaseChecked = async (): Promise<boolean> => {
    return await this.getResponsePhaseCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public checkResponsePhase = async (): Promise<void> => {
    return await this.getResponsePhaseCheckbox().then((checkbox) => checkbox.check());
  };

  public uncheckResponsePhase = async (): Promise<void> => {
    return await this.getResponsePhaseCheckbox().then((checkbox) => checkbox.uncheck());
  };

  public isMessageContentChecked = async (): Promise<boolean> => {
    return await this.getMessageContentCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public isMessageContentDisabled = async (): Promise<boolean> => {
    return await this.getMessageContentCheckbox().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageContent = async (): Promise<void> => {
    return await this.getMessageContentCheckbox().then((checkbox) => checkbox.check());
  };

  public isMessageHeadersChecked = async (): Promise<boolean> => {
    return await this.getMessageHeadersCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public isMessageHeadersDisabled = async (): Promise<boolean> => {
    return await this.getMessageHeadersCheckbox().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageHeaders = async (): Promise<void> => {
    return await this.getMessageHeadersCheckbox().then((checkbox) => checkbox.check());
  };

  public isMessageMetadataChecked = async (): Promise<boolean> => {
    return await this.getMessageMetadataCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public isMessageMetadataDisabled = async (): Promise<boolean> => {
    return await this.getMessageMetadataCheckbox().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageMetadata = async (): Promise<void> => {
    return await this.getMessageMetadataCheckbox().then((checkbox) => checkbox.check());
  };

  public isHeadersChecked = async (): Promise<boolean> => {
    return await this.getHeadersCheckbox().then((checkbox) => checkbox.isChecked());
  };

  public isHeadersDisabled = async (): Promise<boolean> => {
    return await this.getHeadersCheckbox().then((checkbox) => checkbox.isDisabled());
  };

  public checkHeaders = async (): Promise<void> => {
    return await this.getHeadersCheckbox().then((checkbox) => checkbox.check());
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
    const input = await this.getSamplingValueInput();
    await input.setValue(value);
    // Magic hack to mark control as touched. It allows to validate mat-error
    await input.blur();
  };

  public getSamplingValue = async (): Promise<string> => {
    return await this.getSamplingValueInput().then((input) => input.getValue());
  };

  public getSamplingValueErrors = async (): Promise<string[]> => {
    return await this.getSamplingValueFormField().then(async (formField) => {
      return formField.getTextErrors();
    });
  };

  public samplingValueHasErrors = async (): Promise<boolean> => {
    return await this.getSamplingValueFormField().then(async (formField) => {
      return formField.hasErrors();
    });
  };
}
