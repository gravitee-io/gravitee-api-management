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
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class ApiRuntimeLogsProxySettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-proxy-settings';

  private getEntrypointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestPhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="request"]' }));
  private getResponsePhaseCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="response"]' }));
  private getHeadersCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="headers"]' }));
  private getPayloadCheckbox = this.locatorFor(MatCheckboxHarness.with({ selector: '[formControlName="payload"]' }));
  private getConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="condition"]' }));
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  public isEntrypointChecked = async () => (await this.getEntrypointCheckbox()).isChecked();
  public isEntrypointDisabled = async () => (await this.getEntrypointCheckbox()).isDisabled();
  public toggleEntrypoint = async () => (await this.getEntrypointCheckbox()).toggle();
  public isEndpointChecked = async () => (await this.getEndpointCheckbox()).isChecked();
  public isEndpointDisabled = async () => (await this.getEndpointCheckbox()).isDisabled();
  public toggleEndpoint = async () => (await this.getEndpointCheckbox()).toggle();
  public isRequestPhaseChecked = async () => (await this.getRequestPhaseCheckbox()).isChecked();
  public isRequestPhaseDisabled = async () => (await this.getRequestPhaseCheckbox()).isDisabled();
  public toggleRequestPhase = async () => (await this.getRequestPhaseCheckbox()).toggle();
  public isResponsePhaseChecked = async () => (await this.getResponsePhaseCheckbox()).isChecked();
  public isResponsePhaseDisabled = async () => (await this.getResponsePhaseCheckbox()).isDisabled();
  public toggleResponsePhase = async () => (await this.getResponsePhaseCheckbox()).toggle();
  public isHeadersChecked = async () => (await this.getHeadersCheckbox()).isChecked();
  public isHeadersDisabled = async () => (await this.getHeadersCheckbox()).isDisabled();
  public toggleHeaders = async () => (await this.getHeadersCheckbox()).toggle();
  public isPayloadChecked = async () => (await this.getPayloadCheckbox()).isChecked();
  public isPayloadDisabled = async () => (await this.getPayloadCheckbox()).isDisabled();
  public togglePayload = async () => (await this.getPayloadCheckbox()).toggle();
  public getCondition = async () => (await this.getConditionInput()).getValue();
  public isConditionDisabled = async () => (await this.getConditionInput()).isDisabled();
  public setCondition = async (condition: string) => (await this.getConditionInput()).setValue(condition);
  public clickOnSaveButton = async () => (await this.getSaveBar()).clickSubmit();
  public clickOnResetButton = async () => (await this.getSaveBar()).clickReset();
}
