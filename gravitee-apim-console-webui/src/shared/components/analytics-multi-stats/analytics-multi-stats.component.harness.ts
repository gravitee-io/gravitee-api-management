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

export class AnalyticsMultiStatsComponentHarness extends ComponentHarness {
  public static hostSelector = 'analytics-multi-stats';

  private getItems = this.locatorForAll('.analytics-multi-stats__item');
  private getValues = this.locatorForAll('.analytics-multi-stats__value');
  private getLabels = this.locatorForAll('.analytics-multi-stats__label');

  async getStatsItemCount(): Promise<number> {
    const items = await this.getItems();
    return items.length;
  }

  async getAllStatsValues(): Promise<string[]> {
    const values = await this.getValues();
    return Promise.all(values.map(value => value.text()));
  }

  async getAllStatsLabels(): Promise<string[]> {
    const labels = await this.getLabels();
    return Promise.all(labels.map(label => label.text()));
  }

  async getStatsValueAt(index: number): Promise<string> {
    const values = await this.getValues();
    if (index >= values.length) {
      throw new Error(`Stats value at index ${index} not found. Total values: ${values.length}`);
    }
    return values[index].text();
  }

  async getStatsLabelAt(index: number): Promise<string> {
    const labels = await this.getLabels();
    if (index >= labels.length) {
      throw new Error(`Stats label at index ${index} not found. Total labels: ${labels.length}`);
    }
    return labels[index].text();
  }

  async getStatsItemAt(index: number): Promise<{ value: string; label: string }> {
    const value = await this.getStatsValueAt(index);
    const label = await this.getStatsLabelAt(index);
    return { value, label };
  }

  async getAllStatsItems(): Promise<{ value: string; label: string }[]> {
    const itemCount = await this.getStatsItemCount();
    const result = [];

    for (let i = 0; i < itemCount; i++) {
      result.push(await this.getStatsItemAt(i));
    }

    return result;
  }
}
