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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class ApiRuntimeLogsMessageSettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-settings';

  private getEnabledToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  public getLogsBanner = this.locatorFor(DivHarness.with({ text: /Logging smartly/ }));
  private getSaveButton = this.locatorFor(GioSaveBarHarness);
  private getEntrypointToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestPhaseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="request"]' }));
  private getResponsePhaseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="response"]' }));
  private getMessageContentToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="messageContent"]' }));
  private getMessageHeadersToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="messageHeaders"]' }));
  private getMessageMetadataToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="messageMetadata"]' }));
  private getHeadersToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="headers"]' }));
  private getMessageConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="messageCondition"]' }));
  private getRequestConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="requestCondition"]' }));
  private getSamplingTypeToggle = this.locatorFor(MatButtonToggleGroupHarness.with({ selector: '[formControlName="samplingType"]' }));
  public getSamplingValueInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="samplingValue"]' }));
  private getSamplingValueFormField = this.locatorFor(MatFormFieldHarness.with({ selector: '[data-testid=sampling_value]' }));
  private getTracingEnabledToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="tracingEnabled"]' }));
  private getTracingVerboseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="tracingVerbose"]' }));

  public isEnabledChecked = async () => (await this.getEnabledToggle()).isChecked();
  public toggleEnabled = async () => (await this.getEnabledToggle()).toggle();

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
    return await this.getEntrypointToggle().then((checkbox) => checkbox.isChecked());
  };
  public isEntrypointDisabled = async () => (await this.getEntrypointToggle()).isDisabled();

  public toggleEntrypoint = async (): Promise<void> => {
    return await this.getEntrypointToggle().then((checkbox) => checkbox.toggle());
  };

  public isEndpointChecked = async (): Promise<boolean> => {
    return await this.getEndpointToggle().then((checkbox) => checkbox.isChecked());
  };
  public isEndpointDisabled = async () => (await this.getEndpointToggle()).isDisabled();

  public toggleEndpoint = async (): Promise<void> => {
    return await this.getEndpointToggle().then((checkbox) => checkbox.toggle());
  };

  public isRequestPhaseChecked = async (): Promise<boolean> => {
    return await this.getRequestPhaseToggle().then((checkbox) => checkbox.isChecked());
  };

  public checkRequestPhase = async (): Promise<void> => {
    return await this.getRequestPhaseToggle().then((checkbox) => checkbox.check());
  };

  public uncheckRequestPhase = async (): Promise<void> => {
    return await this.getRequestPhaseToggle().then((checkbox) => checkbox.uncheck());
  };

  public isResponsePhaseChecked = async (): Promise<boolean> => {
    return await this.getResponsePhaseToggle().then((checkbox) => checkbox.isChecked());
  };

  public checkResponsePhase = async (): Promise<void> => {
    return await this.getResponsePhaseToggle().then((checkbox) => checkbox.check());
  };

  public uncheckResponsePhase = async (): Promise<void> => {
    return await this.getResponsePhaseToggle().then((checkbox) => checkbox.uncheck());
  };

  public isMessageContentChecked = async (): Promise<boolean> => {
    return await this.getMessageContentToggle().then((checkbox) => checkbox.isChecked());
  };

  public isMessageContentDisabled = async (): Promise<boolean> => {
    return await this.getMessageContentToggle().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageContent = async (): Promise<void> => {
    return await this.getMessageContentToggle().then((checkbox) => checkbox.check());
  };

  public isMessageHeadersChecked = async (): Promise<boolean> => {
    return await this.getMessageHeadersToggle().then((checkbox) => checkbox.isChecked());
  };

  public isMessageHeadersDisabled = async (): Promise<boolean> => {
    return await this.getMessageHeadersToggle().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageHeaders = async (): Promise<void> => {
    return await this.getMessageHeadersToggle().then((checkbox) => checkbox.check());
  };

  public isMessageMetadataChecked = async (): Promise<boolean> => {
    return await this.getMessageMetadataToggle().then((checkbox) => checkbox.isChecked());
  };

  public isMessageMetadataDisabled = async (): Promise<boolean> => {
    return await this.getMessageMetadataToggle().then((checkbox) => checkbox.isDisabled());
  };

  public checkMessageMetadata = async (): Promise<void> => {
    return await this.getMessageMetadataToggle().then((checkbox) => checkbox.check());
  };

  public isHeadersChecked = async (): Promise<boolean> => {
    return await this.getHeadersToggle().then((checkbox) => checkbox.isChecked());
  };

  public isHeadersDisabled = async (): Promise<boolean> => {
    return await this.getHeadersToggle().then((checkbox) => checkbox.isDisabled());
  };

  public checkHeaders = async (): Promise<void> => {
    return await this.getHeadersToggle().then((checkbox) => checkbox.check());
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

  public isTracingEnabledChecked = async () => (await this.getTracingEnabledToggle()).isChecked();
  public toggleTracingEnabled = async () => (await this.getTracingEnabledToggle()).toggle();
  public isTracingVerboseChecked = async () => (await this.getTracingVerboseToggle()).isChecked();
  public toggleTracingVerbose = async () => (await this.getTracingVerboseToggle()).toggle();
}
