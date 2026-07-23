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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export interface RenderedHeader {
  name: string;
  value: string;
}

export class FailedHealthCheckDetailsDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'failed-health-check-details-dialog';

  private readonly closeButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="close-button"]' }));

  private readonly stepPanels = this.locatorForAll(MatExpansionPanelHarness);
  private readonly stepMessages = this.locatorForAll(DivHarness.with({ selector: '[data-testid="step-message"]' }));

  private readonly requestNoHeaders = this.locatorForAll(DivHarness.with({ selector: '[data-testid="request-no-headers"]' }));
  private readonly responseNoHeaders = this.locatorForAll(DivHarness.with({ selector: '[data-testid="response-no-headers"]' }));

  private readonly bodyAccordions = this.locatorForAll('body-accordion');
  private readonly noDetails = this.locatorForAll(DivHarness.with({ selector: '[data-testid="no-details"]' }));

  async getStepCount(): Promise<number> {
    return (await this.stepPanels()).length;
  }

  async getStepNames(): Promise<string[]> {
    const panels = await this.stepPanels();
    return parallel(() => panels.map((panel) => panel.getTitle()));
  }

  async getStepMessages(): Promise<string[]> {
    return this.collectTexts(this.stepMessages);
  }

  async getSummaryValue(field: 'timestamp' | 'endpoint' | 'gateway' | 'response-time'): Promise<string> {
    return this.getTextByTestId(`summary-${field}`);
  }

  async getRequestMethod(): Promise<string> {
    return this.getTextByTestId('request-method');
  }

  async getRequestUri(): Promise<string> {
    return this.getTextByTestId('request-uri');
  }

  async getResponseStatus(): Promise<string> {
    return this.getTextByTestId('response-status');
  }

  async getRequestHeaders(): Promise<RenderedHeader[]> {
    return this.collectHeaders('request-header');
  }

  async getResponseHeaders(): Promise<RenderedHeader[]> {
    return this.collectHeaders('response-header');
  }

  async hasRequestNoHeadersMessage(): Promise<boolean> {
    return (await this.requestNoHeaders()).length > 0;
  }

  async hasResponseNoHeadersMessage(): Promise<boolean> {
    return (await this.responseNoHeaders()).length > 0;
  }

  async hasBodySection(): Promise<boolean> {
    return (await this.bodyAccordions()).length > 0;
  }

  async hasNoDetailsMessage(): Promise<boolean> {
    return (await this.noDetails()).length > 0;
  }

  async close(): Promise<void> {
    return this.closeButton().then((button) => button.click());
  }

  private async getTextByTestId(testId: string): Promise<string> {
    const element = await this.locatorFor(DivHarness.with({ selector: `[data-testid="${testId}"]` }))();
    return element.getText();
  }

  /** Name and value live in sibling elements, so they are collected separately then paired by position. */
  private async collectHeaders(rowTestId: string): Promise<RenderedHeader[]> {
    const names = await this.collectTexts(
      this.locatorForAll(DivHarness.with({ selector: `[data-testid="${rowTestId}"] [data-testid="header-name"]` })),
    );
    const values = await this.collectTexts(
      this.locatorForAll(DivHarness.with({ selector: `[data-testid="${rowTestId}"] [data-testid="header-value"]` })),
    );

    return names.map((name, index) => ({ name: name.replace(/:$/, ''), value: values[index] }));
  }

  private async collectTexts(locator: () => Promise<DivHarness[]>): Promise<string[]> {
    const elements = await locator();
    return parallel(() => elements.map((element) => element.getText()));
  }
}
