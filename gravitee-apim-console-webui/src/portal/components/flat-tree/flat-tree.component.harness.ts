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
import { MatMenuItemHarness } from '@angular/material/menu/testing';
import { MatTreeHarness, MatTreeNodeHarness } from '@angular/material/tree/testing';
import { MatIconHarness } from '@angular/material/icon/testing';
import { MatDividerHarness } from '@angular/material/divider/testing';

import { EmptyStateComponentHarness } from '../../../shared/components/empty-state/empty-state.component.harness';

export class FlatTreeComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'portal-flat-tree-component';

  private readonly _documentRootLocator = this.documentRootLocatorFactory();

  private readonly getTree = this.locatorForOptional(MatTreeHarness);
  private readonly getAllNodes = this.locatorForAll(MatTreeNodeHarness);
  private readonly getEmptyState = this.locatorForOptional(EmptyStateComponentHarness);
  private readonly getEditButton = this._documentRootLocator.locatorFor(
    MatMenuItemHarness.with({ selector: '[data-testid="edit-node-button"]' }),
  );
  protected getMoreActionsButtonById = (id: string) =>
    this.locatorForOptional(MatButtonHarness.with({ selector: `[data-testid="more-actions-${id}"]` }));

  private async getSelectedNode(): Promise<MatTreeNodeHarness | null> {
    const tree = await this.getTree();
    if (!tree) {
      return null;
    }
    const nodes = await tree.getNodes();

    for (const node of nodes) {
      const host = await node.host();
      const hasSelectedClass = await host.hasClass('selected');
      if (hasSelectedClass) {
        return node;
      }
    }

    return null;
  }

  private async getNodeLabelText(node: MatTreeNodeHarness): Promise<string> {
    const text = await node.getText();
    return text.trim();
  }

  async getSelectedItemTitle(): Promise<string | null> {
    const selectedNode = await this.getSelectedNode();
    if (!selectedNode) {
      return null;
    }
    return this.getNodeLabelText(selectedNode);
  }

  async getSelectedItemType(): Promise<string | null> {
    const selectedNode = await this.getSelectedNode();
    if (!selectedNode) {
      return null;
    }
    const iconType = await selectedNode.getHarness(MatIconHarness.with({ selector: '.tree__icon__type' }));
    return iconType.getName();
  }

  async getNodeHarnessByTitle(title: string): Promise<MatTreeNodeHarness> {
    const tree = await this.getTree();
    if (!tree) throw new Error('Tree not found');

    const nodes = await tree.getNodes();
    for (const node of nodes) {
      const labelText = await node.getText();
      if (labelText.trim() === title.trim()) {
        return node;
      }
    }
    throw new Error(`Node with title "${title}" not found.`);
  }

  async selectItemByTitle(title: string): Promise<void> {
    const node = await this.getNodeHarnessByTitle(title);
    return node.host().then(host => host.click());
  }

  async getAllItemTitles(): Promise<string[]> {
    const tree = await this.getTree();
    if (!tree) {
      return [];
    }

    const nodes = await this.getAllNodes();
    const titles: string[] = [];

    for (const node of nodes) {
      const labelText = await this.getNodeLabelText(node);
      titles.push(labelText.trim());
    }

    return titles;
  }

  async isEmptyStateDisplayed(): Promise<boolean> {
    const emptyState = await this.getEmptyState();
    return emptyState !== null;
  }

  async selectEditById(id: string): Promise<void> {
    const moreActionsButton = await this.getMoreActionsButtonById(id)();
    return moreActionsButton
      .click()
      .then(_ => this.getEditButton())
      .then(editButton => editButton.click());
  }

  async selectDeleteById(id: string): Promise<void> {
    const moreActionsButton = await this.getMoreActionsButtonById(id)();
    const deleteButton = this._documentRootLocator.locatorFor(MatMenuItemHarness.with({ selector: `[data-testid="delete-node-button"]` }));
    return moreActionsButton
      .click()
      .then(_ => deleteButton())
      .then(deleteBtn => deleteBtn.click());
  }

  async selectPublishById(id: string): Promise<void> {
    const moreActionsButton = await this.getMoreActionsButtonById(id)();
    await moreActionsButton.click();
    const publishButton = await this._documentRootLocator.locatorFor(
      MatMenuItemHarness.with({ selector: `[data-testid="publish-node-button"]` }),
    )();
    await publishButton.click();
  }

  async selectUnpublishById(id: string): Promise<void> {
    const moreActionsButton = await this.getMoreActionsButtonById(id)();
    await moreActionsButton.click();
    const unpublishButton = await this._documentRootLocator.locatorFor(
      MatMenuItemHarness.with({ selector: `[data-testid="unpublish-node-button"]` }),
    )();
    await unpublishButton.click();
  }

  async getMenuItemByText(text: string): Promise<MatMenuItemHarness | null> {
    return this._documentRootLocator.locatorForOptional(MatMenuItemHarness.with({ text }))();
  }

  async getMenuItemByTestId(testId: string): Promise<MatMenuItemHarness | null> {
    return this._documentRootLocator.locatorForOptional(MatMenuItemHarness.with({ selector: `[data-testid="${testId}"]` }))();
  }

  async hasDivider(): Promise<boolean> {
    return (await this._documentRootLocator.locatorForOptional(MatDividerHarness)()) !== null;
  }
}
