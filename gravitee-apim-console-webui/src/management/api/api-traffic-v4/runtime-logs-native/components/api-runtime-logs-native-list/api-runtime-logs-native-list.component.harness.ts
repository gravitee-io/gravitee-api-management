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

export interface StatusBadge {
  text: string;
  classes: string[];
}

export class ApiRuntimeLogsNativeListHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-native-list';

  private readonly statusBadgeElements = this.locatorForAll('td.mat-column-connectionStatus span');
  private readonly durationCells = this.locatorForAll('td.mat-column-duration');
  private readonly viewButtons = this.locatorForAll('td.mat-column-view button');

  async getStatusBadges(): Promise<StatusBadge[]> {
    const spans = await this.statusBadgeElements();
    return Promise.all(
      spans.map(async span => ({
        text: (await span.text()).trim(),
        classes: ((await span.getAttribute('class')) ?? '').split(/\s+/).filter(Boolean),
      })),
    );
  }

  async getDurationTexts(): Promise<string[]> {
    const cells = await this.durationCells();
    return Promise.all(cells.map(async cell => (await cell.text()).trim()));
  }

  async getViewButtonCount(): Promise<number> {
    return (await this.viewButtons()).length;
  }
}
