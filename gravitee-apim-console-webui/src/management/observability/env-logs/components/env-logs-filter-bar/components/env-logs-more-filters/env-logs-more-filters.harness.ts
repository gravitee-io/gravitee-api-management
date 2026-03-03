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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class EnvLogsMoreFiltersHarness extends ComponentHarness {
  static hostSelector = 'env-logs-more-filters';

  private getClearAllButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="more-filters-clear"]' }));
  private getApplyButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="more-filters-apply"]' }));
  private getCloseButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="more-filters-close"]' }));
  private getPeriodSelect = this.locatorForOptional(MatSelectHarness.with({ ancestor: '[data-testid="more-filters-period"]' }));
  private getStatusInput = this.locatorForOptional('[data-testid="more-filters-status-input"]');
  private getEntrypointsSelect = this.locatorForOptional(MatSelectHarness.with({ ancestor: '[data-testid="more-filters-entrypoints"]' }));
  private getMethodsSelect = this.locatorForOptional(MatSelectHarness.with({ ancestor: '[data-testid="more-filters-methods"]' }));
  private getPlansSelect = this.locatorForOptional(MatSelectHarness.with({ ancestor: '[data-testid="more-filters-plans"]' }));
  private getMcpMethodInput = this.locatorForOptional(MatInputHarness.with({ ancestor: '[data-testid="more-filters-mcp-method"]' }));
  private getTransactionIdInput = this.locatorForOptional(
    MatInputHarness.with({ ancestor: '[data-testid="more-filters-transaction-id"]' }),
  );
  private getRequestIdInput = this.locatorForOptional(MatInputHarness.with({ ancestor: '[data-testid="more-filters-request-id"]' }));
  private getUriInput = this.locatorForOptional(MatInputHarness.with({ ancestor: '[data-testid="more-filters-uri"]' }));
  private getResponseTimeInput = this.locatorForOptional(MatInputHarness.with({ ancestor: '[data-testid="more-filters-response-time"]' }));

  async clickClearAll(): Promise<void> {
    return (await this.getClearAllButton()).click();
  }

  async clickApply(): Promise<void> {
    return (await this.getApplyButton()).click();
  }

  async clickClose(): Promise<void> {
    return (await this.getCloseButton()).click();
  }

  async isApplyDisabled(): Promise<boolean> {
    return (await this.getApplyButton()).isDisabled();
  }

  async getPeriod(): Promise<MatSelectHarness | null> {
    return this.getPeriodSelect();
  }

  async hasStatusInput(): Promise<boolean> {
    return (await this.getStatusInput()) !== null;
  }

  async getEntrypoints(): Promise<MatSelectHarness | null> {
    return this.getEntrypointsSelect();
  }

  async getMethods(): Promise<MatSelectHarness | null> {
    return this.getMethodsSelect();
  }

  async getPlans(): Promise<MatSelectHarness | null> {
    return this.getPlansSelect();
  }

  async getMcpMethod(): Promise<MatInputHarness | null> {
    return this.getMcpMethodInput();
  }

  async getTransactionId(): Promise<MatInputHarness | null> {
    return this.getTransactionIdInput();
  }

  async getRequestId(): Promise<MatInputHarness | null> {
    return this.getRequestIdInput();
  }

  async getUri(): Promise<MatInputHarness | null> {
    return this.getUriInput();
  }

  async getResponseTime(): Promise<MatInputHarness | null> {
    return this.getResponseTimeInput();
  }
}
