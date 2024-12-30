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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class ApiRuntimeLogsProxySettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-proxy-settings';

  private getEnabledToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  private getEntrypointToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="entrypoint"]' }));
  private getEndpointToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="endpoint"]' }));
  private getRequestPhaseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="request"]' }));
  private getResponsePhaseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="response"]' }));
  private getHeadersToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="headers"]' }));
  private getPayloadToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="payload"]' }));
  private getTracingEnabledToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="tracingEnabled"]' }));
  private getTracingVerboseToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="tracingVerbose"]' }));
  private getConditionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="condition"]' }));
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  public isEnabledChecked = async () => (await this.getEnabledToggle()).isChecked();
  public toggleEnabled = async () => (await this.getEnabledToggle()).toggle();
  public isEntrypointChecked = async () => (await this.getEntrypointToggle()).isChecked();
  public isEntrypointDisabled = async () => (await this.getEntrypointToggle()).isDisabled();
  public toggleEntrypoint = async () => (await this.getEntrypointToggle()).toggle();
  public isEndpointChecked = async () => (await this.getEndpointToggle()).isChecked();
  public isEndpointDisabled = async () => (await this.getEndpointToggle()).isDisabled();
  public toggleEndpoint = async () => (await this.getEndpointToggle()).toggle();
  public isRequestPhaseChecked = async () => (await this.getRequestPhaseToggle()).isChecked();
  public isRequestPhaseDisabled = async () => (await this.getRequestPhaseToggle()).isDisabled();
  public toggleRequestPhase = async () => (await this.getRequestPhaseToggle()).toggle();
  public isResponsePhaseChecked = async () => (await this.getResponsePhaseToggle()).isChecked();
  public isResponsePhaseDisabled = async () => (await this.getResponsePhaseToggle()).isDisabled();
  public toggleResponsePhase = async () => (await this.getResponsePhaseToggle()).toggle();
  public isHeadersChecked = async () => (await this.getHeadersToggle()).isChecked();
  public isHeadersDisabled = async () => (await this.getHeadersToggle()).isDisabled();
  public toggleHeaders = async () => (await this.getHeadersToggle()).toggle();
  public isPayloadChecked = async () => (await this.getPayloadToggle()).isChecked();
  public isPayloadDisabled = async () => (await this.getPayloadToggle()).isDisabled();
  public togglePayload = async () => (await this.getPayloadToggle()).toggle();
  public getCondition = async () => (await this.getConditionInput()).getValue();
  public isConditionDisabled = async () => (await this.getConditionInput()).isDisabled();
  public setCondition = async (condition: string) => (await this.getConditionInput()).setValue(condition);
  public clickOnSaveButton = async () => (await this.getSaveBar()).clickSubmit();
  public clickOnResetButton = async () => (await this.getSaveBar()).clickReset();

  public isTracingEnabledChecked = async () => (await this.getTracingEnabledToggle()).isChecked();
  public toggleTracingEnabled = async () => (await this.getTracingEnabledToggle()).toggle();
  public isTracingVerboseChecked = async () => (await this.getTracingVerboseToggle()).isChecked();
  public isTracingVerboseDisabled = async () => (await this.getTracingVerboseToggle()).isDisabled();
  public toggleTracingVerbose = async () => (await this.getTracingVerboseToggle()).toggle();
}
