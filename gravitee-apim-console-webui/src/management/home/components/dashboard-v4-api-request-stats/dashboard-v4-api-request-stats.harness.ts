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

export class DashboardV4ApiRequestStatsHarness extends ComponentHarness {
  static hostSelector = '.stats';
  protected getRows = this.locatorForAll('.stats__body__row');

  async getMinResponseTime(): Promise<string> {
    return this.getValueByLabel('Min');
  }

  async getMaxResponseTime(): Promise<string> {
    return this.getValueByLabel('Max');
  }

  async getAverageResponseTime(): Promise<string> {
    return this.getValueByLabel('Average');
  }

  async getRequestsPerSecond(): Promise<string> {
    return this.getValueByLabel('Per second');
  }

  async getTotalRequests(): Promise<string> {
    return this.getValueByLabel('Total');
  }

  private async getValueByLabel(label: string): Promise<string> {
    return this.getRows()
      .then((rows) => Promise.all(rows.map((t) => t.text())))
      .then((res) => res.find((t) => t.includes(label)))
      .then((found) => found.replace(label, ''));
  }
}
