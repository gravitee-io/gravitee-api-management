/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { MatProgressBarHarness } from '@angular/material/progress-bar/testing';
import { MatTableHarness } from '@angular/material/table/testing';

export class ConsumerGroupDetailHarness extends ComponentHarness {
  static hostSelector = 'gke-consumer-group-detail';

  private readonly getTables = this.locatorForAll(MatTableHarness);
  private readonly getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[mat-icon-button]' }));
  private readonly getProgressBar = this.locatorForOptional(MatProgressBarHarness);
  private readonly getTitle = this.locatorFor('.consumer-group-detail__title');
  private readonly getStateBadge = this.locatorForOptional('.consumer-group-detail__state-badge');

  async getGroupId() {
    const title = await this.getTitle();
    return title.text();
  }

  async getState() {
    const badge = await this.getStateBadge();
    return badge ? badge.text() : null;
  }

  async isLoading() {
    return (await this.getProgressBar()) !== null;
  }

  async clickBack() {
    const button = await this.getBackButton();
    await button.click();
  }

  async getMembersRows() {
    const tables = await this.getTables();
    if (tables.length < 1) return [];
    const rows = await tables[0].getRows();
    return parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  async getOffsetsRows() {
    const tables = await this.getTables();
    if (tables.length < 2) return [];
    const rows = await tables[1].getRows();
    return parallel(() => rows.map(row => row.getCellTextByColumnName()));
  }

  async clickOffsetTopicLink(rowIndex: number) {
    const tables = await this.getTables();
    const allLinks = await this.locatorForAll('.consumer-group-detail__topic-link')();
    // Each member row may have multiple assignment links; use a simpler approach:
    // offset topic links start after all assignment topic links
    // For simplicity, get the offset table rows count and index from the end
    const offsetsRows = tables.length > 1 ? await tables[1].getRows() : [];
    const offsetLinksStart = allLinks.length - offsetsRows.length;
    const link = allLinks[offsetLinksStart + rowIndex];
    if (link) {
      await link.click();
    }
  }
}
