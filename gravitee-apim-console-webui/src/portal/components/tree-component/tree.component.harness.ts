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

import { EmptyStateComponentHarness } from '../../../shared/components/empty-state/empty-state.component.harness';

export class TreeComponentHarness extends ComponentHarness {
  static hostSelector = 'portal-tree-component';

  private getSelectedLabelButton = this.locatorForOptional('.tree__row.selected .tree__label');
  private getTreeLabelButtons = this.locatorForAll('.tree__label');
  private getEmptyState = this.locatorForOptional(EmptyStateComponentHarness);

  async getSelectedItemTitle(): Promise<string | null> {
    const labelButton = await this.getSelectedLabelButton();
    if (!labelButton) {
      return null;
    }
    return labelButton.text();
  }

  async getSelectedItemType(): Promise<string | null> {
    const selectedIcon = await this.locatorForOptional('.tree__row.selected .tree__icon')();
    if (!selectedIcon) {
      return null;
    }
    return selectedIcon.getAttribute('data-test-type');
  }

  async selectItemByTitle(title: string): Promise<void> {
    const buttons = await this.getTreeLabelButtons();

    for (const button of buttons) {
      const text = await button.text();
      if (text.trim() === title.trim()) {
        return button.click();
      }
    }

    throw new Error(`No item found with title: ${title}`);
  }

  async getAllItemTitles(): Promise<string[]> {
    const buttons = await this.getTreeLabelButtons();
    const titles: string[] = [];

    for (const button of buttons) {
      const text = await button.text();
      titles.push(text.trim());
    }

    return titles;
  }

  async isEmptyStateDisplayed(): Promise<boolean> {
    const emptyState = await this.getEmptyState();
    return emptyState !== null;
  }
}
