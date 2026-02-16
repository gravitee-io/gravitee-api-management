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
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';

export class ApiRuntimeLogsConnectionLogDetailsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-connection-log-details';

  public entrypointRequestPanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=entrypoint-request]' }));
  public endpointRequestPanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=endpoint-request]' }));
  public entrypointResponsePanelSelector = this.locatorFor(
    MatExpansionPanelHarness.with({ selector: '[data-testId=entrypoint-response]' }),
  );
  public endpointResponsePanelSelector = this.locatorFor(MatExpansionPanelHarness.with({ selector: '[data-testId=endpoint-response]' }));
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

  public getConnectionLogBody = async (panel: MatExpansionPanelHarness): Promise<string> => {
    return panel.getHarness(DivHarness.with({ selector: '.accordion__body' })).then(body => body.getText());
  };
}
