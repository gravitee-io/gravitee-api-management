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

import { TreeNodeComponentHarness } from './tree-node.component.harness';
import { DivHarness } from '../../../../../testing/div.harness';

export class TreeComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-tree-component';

  private readonly getTreeNodes = this.locatorForAll(TreeNodeComponentHarness);

  private readonly getTreeLabelButtons = this.locatorForAll('.tree__label');

  private readonly getFolderItem = this.locatorForAll(DivHarness.with({ selector: '.tree__row.folder' }));

  async getTopLevelNodes(): Promise<TreeNodeComponentHarness[]> {
    const nodes = await this.getTreeNodes();
    const nodeLevels = await Promise.all(nodes.map(node => node.isTopLevel()));
    return nodes.filter((node, index) => nodeLevels[index]);
  }

  async getSelectedItem() {
    const nodes = await this.getTreeNodes();
    const nodeTexts = await Promise.all(nodes.map(node => node.isSelected()));
    return nodes.find((node, index) => nodeTexts[index]);
  }

  async getFolderByTitle(title: string): Promise<{
    label: string;
    expanded: boolean;
  } | null> {
    const buttons = await this.getFolderItem();

    for (const button of buttons) {
      const label = await button.childLocatorForOptional('.tree__label')();
      const text = await label?.text();
      if (text?.trim() === title.trim()) {
        const icon = await button.childLocatorForOptional('.tree__icon')();
        if (icon) {
          return {
            label: text,
            expanded: await icon.hasClass('expanded'),
          };
        }
      }
    }

    throw new Error(`No item found with title: ${title}`);
  }

  async clickItemByTitle(title: string): Promise<void> {
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
}
