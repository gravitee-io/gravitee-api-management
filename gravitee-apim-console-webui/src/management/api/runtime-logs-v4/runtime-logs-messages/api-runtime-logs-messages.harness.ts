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
import { MatIconHarness } from '@angular/material/icon/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatTabGroupHarness, MatTabHarness } from '@angular/material/tabs/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';

export class ApiRuntimeLogsMessagesHarness extends ComponentHarness {
  static hostSelector = 'runtime-logs-messages';

  private entrypointTabBodySelector = this.locatorFor(DivHarness.with({ selector: '[data-testId=entrypoint-tabs-body]' }));
  private endpointTabBodySelector = this.locatorFor(DivHarness.with({ selector: '[data-testId=endpoint-tabs-body]' }));
  private load5MoreSelector = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Load 5 more"]' }));
  private messagesSelector = this.locatorForAll(DivHarness.with({ selector: '.log__header__title' }));
  private entrypointTabGroupSelector = this.locatorFor(MatTabGroupHarness.with({ selector: '[data-testId=entrypoint-tabs-group]' }));
  private endpointTabGroupSelector = this.locatorFor(MatTabGroupHarness.with({ selector: '[data-testId=endpoint-tabs-group]' }));
  private connectionLogsTabSelector = this.locatorFor(MatTabHarness.with({ label: 'Connection Logs' }));
  private messagesTabSelector = this.locatorFor(MatTabHarness.with({ label: 'Messages' }));
  public entrypointRequestPanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=entrypoint-request]' }));
  public endpointRequestPanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=endpoint-request]' }));
  public entrypointResponsePanelSelector = this.locatorFor(
    MatExpansionPanelHarness.with({ selector: '[data-testId=entrypoint-response]' }),
  );
  public endpointResponsePanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=endpoint-response]' }));

  public entrypointConnectorIcon = this.locatorFor(MatIconHarness.with({ selector: '[data-testId=entrypoint-connector-icon]' }));
  public endpointConnectorIcon = this.locatorFor(MatIconHarness.with({ selector: '[data-testId=endpoint-connector-icon]' }));

  public clickOnConnectionLogsTab = async () => {
    return this.connectionLogsTabSelector().then((tab) => tab.select());
  };

  public clickOnMessagesTab = async () => {
    return this.messagesTabSelector().then((tab) => tab.select());
  };

  public getConnectionLogRequestUri = async (panel: MatExpansionPanelHarness) => {
    const method = await panel.getHarness(DivHarness.with({ selector: '[data-testId=uri-value]' })).catch(() => null);
    return method?.getText();
  };

  public getConnectionLogRequestMethod = async (panel: MatExpansionPanelHarness) => {
    const method = await panel.getHarness(DivHarness.with({ selector: '[data-testId=method-value]' }));
    return method.getText();
  };

  public getConnectionLogResponseStatus = async (panel: MatExpansionPanelHarness) => {
    const method = await panel.getHarness(DivHarness.with({ selector: '[data-testId=status-value]' }));
    return method.getText();
  };

  public getConnectionLogHeaders = async (panel: MatExpansionPanelHarness): Promise<Record<string, string>> => {
    const headersHarnesses = await panel.getAllHarnesses(DivHarness.with({ selector: '[data-testId=header-value]' }));
    let headers: Record<string, string> = {};
    for (const headersHarness of headersHarnesses) {
      const headerKey = await headersHarness.getText({ childSelector: '.log__row__property' });
      const headerValue = await headersHarness.getText({ childSelector: '.log__row__value' });
      headers = { ...headers, [headerKey.slice(0, -1)]: headerValue };
    }
    return headers;
  };

  public clickOnEntrypointTab = async (label: string) => {
    return this.entrypointTabGroupSelector().then((tabGroup) => tabGroup.selectTab({ label: label }));
  };
  public clickOnEndpointTab = async (label: string) => {
    return this.endpointTabGroupSelector().then((tabGroup) => tabGroup.selectTab({ label: label }));
  };

  public getEntrypointTabBody = async () => {
    return await this.entrypointTabBodySelector().then((tabBody) => tabBody.getText());
  };
  public getEndpointTabBody = async () => {
    return await this.endpointTabBodySelector().then((tabBody) => tabBody.getText());
  };

  public load5More = async () => {
    return await this.load5MoreSelector().then((tab) => tab.click());
  };

  public getMessages = () => {
    return this.messagesSelector();
  };
}
