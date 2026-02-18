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
import { GraviteeMarkdownEditorHarness } from '@gravitee/gravitee-markdown';

import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatMenuHarness, MatMenuItemHarness } from '@angular/material/menu/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

import { EmptyStateComponentHarness } from '../../shared/components/empty-state/empty-state.component.harness';
import { FlatTreeComponentHarness } from '../components/flat-tree/flat-tree.component.harness';

export class PortalNavigationItemsHarness extends ComponentHarness {
  static hostSelector = 'portal-navigation-items';

  private getAddButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Add new section"]' }));
  private getEditButton = this.locatorFor(MatButtonHarness.with({ text: /Edit/i }));
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ text: /Save/i }));
  private getPublishButton = this.locatorForOptional(MatButtonHarness.with({ text: /^Publish$/ }));
  private getUnpublishButton = this.locatorForOptional(MatButtonHarness.with({ text: /^Unpublish$/ }));
  private getMenu = this.locatorFor(MatMenuHarness);
  private getTree = this.locatorFor(FlatTreeComponentHarness);
  private getGraviteeMarkdownEditor = this.locatorFor(GraviteeMarkdownEditorHarness);
  private getEmptyEditor = this.locatorForOptional(
    EmptyStateComponentHarness.with({
      title: 'Editor',
      message: 'Use GMD code to customize and edit your page content.',
    }),
  );
  private getTitle = this.locatorFor(DivHarness.with({ selector: '.panel-header__title' }));
  private getPageNotFoundEmptyState = this.locatorForOptional(
    EmptyStateComponentHarness.with({
      title: 'Page Not Found',
      message: 'Failed to load page content.',
    }),
  );

  async getAddButtonHarness(): Promise<MatButtonHarness> {
    return this.getAddButton();
  }

  async clickAddButton(): Promise<void> {
    const button = await this.getAddButton();
    return button.click();
  }

  async clickEditButton(): Promise<void> {
    const button = await this.getEditButton();
    return button.click();
  }

  async getPageMenuItem(): Promise<MatMenuItemHarness> {
    const menu = await this.getMenu();
    return menu.getHarness(MatMenuItemHarness.with({ text: /Add Page/i }));
  }

  async getLinkMenuItem(): Promise<MatMenuItemHarness> {
    const menu = await this.getMenu();
    return menu.getHarness(MatMenuItemHarness.with({ text: /Add Link/i }));
  }

  async getFolderMenuItem(): Promise<MatMenuItemHarness> {
    const menu = await this.getMenu();
    return menu.getHarness(MatMenuItemHarness.with({ text: /Add Folder/i }));
  }

  async getApiMenuItem(): Promise<MatMenuItemHarness> {
    const menu = await this.getMenu();
    return menu.getHarness(MatMenuItemHarness.with({ text: /Add API/i }));
  }

  async clickPageMenuItem(): Promise<void> {
    const menuItem = await this.getPageMenuItem();
    return menuItem.click();
  }

  async clickLinkMenuItem(): Promise<void> {
    const menuItem = await this.getLinkMenuItem();
    return menuItem.click();
  }

  async clickFolderMenuItem(): Promise<void> {
    const menuItem = await this.getFolderMenuItem();
    return menuItem.click();
  }

  async clickApiMenuItem(): Promise<void> {
    const menuItem = await this.getApiMenuItem();
    return menuItem.click();
  }

  async getSelectedNavigationItemTitle(): Promise<string> {
    const tree = await this.getTree();
    return tree.getSelectedItemTitle();
  }

  async getSelectedNavigationItemType(): Promise<string> {
    const tree = await this.getTree();
    return tree.getSelectedItemType();
  }

  async selectNavigationItemByTitle(title: string): Promise<void> {
    const tree = await this.getTree();
    return tree.selectItemByTitle(title);
  }

  async getNavigationItemTitles(): Promise<string[]> {
    const tree = await this.getTree();
    return tree.getAllItemTitles();
  }

  async getEditorContentText(): Promise<string> {
    const editor = await this.getGraviteeMarkdownEditor();
    return editor.getEditorValue();
  }

  async setEditorContentText(content: string): Promise<void> {
    const editor = await this.getGraviteeMarkdownEditor();
    return editor.setEditorValue(content);
  }

  async isEditorEmptyStateDisplayed(): Promise<boolean> {
    const emptyState = await this.getEmptyEditor();
    return emptyState !== null;
  }

  async isNavigationTreeEmpty(): Promise<boolean> {
    const tree = await this.getTree();
    return tree.isEmptyStateDisplayed();
  }

  async isSaveButtonDisabled(): Promise<boolean> {
    const button = await this.getSaveButton();
    return button.isDisabled();
  }

  async isPublishButtonVisible(): Promise<boolean> {
    const button = await this.getPublishButton();
    return button !== null;
  }

  async clickPublishButton(): Promise<void> {
    const button = await this.getPublishButton();
    return button.click();
  }

  async isUnpublishButtonVisible(): Promise<boolean> {
    const button = await this.getUnpublishButton();
    return button !== null;
  }

  async clickUnpublishButton(): Promise<void> {
    const button = await this.getUnpublishButton();
    return button.click();
  }

  async isPublishedBadgeVisible(): Promise<boolean> {
    const titleDiv = await this.getTitle();
    const titleText = await titleDiv.getText();
    return titleText.includes('Published');
  }

  async isUnpublishedBadgeVisible(): Promise<boolean> {
    const titleDiv = await this.getTitle();
    const titleText = await titleDiv.getText();
    return titleText.includes('Unpublished');
  }

  async isPublicBadgeVisible() {
    const titleDiv = await this.getTitle();
    const titleText = await titleDiv.getText();
    return titleText.includes('Public');
  }

  async isPrivateBadgeVisible(): Promise<boolean> {
    const titleDiv = await this.getTitle();
    const titleText = await titleDiv.getText();
    return titleText.includes('Private');
  }

  async editNodeById(id: string): Promise<void> {
    const tree = await this.getTree();
    return tree.selectEditById(id);
  }

  async deleteNodeById(id: string): Promise<void> {
    const tree = await this.getTree();
    return tree.selectDeleteById(id);
  }

  async publishNodeById(id: string): Promise<void> {
    const tree = await this.getTree();
    return tree.selectPublishById(id);
  }

  async unpublishNodeById(id: string): Promise<void> {
    const tree = await this.getTree();
    return tree.selectUnpublishById(id);
  }

  async isPageNotFoundDisplayed(): Promise<boolean> {
    const emptyState = await this.getPageNotFoundEmptyState();
    return emptyState !== null;
  }

  async getPageNotFoundMessage(): Promise<string | null> {
    const emptyState = await this.getPageNotFoundEmptyState();
    return emptyState ? emptyState.getMessage() : null;
  }
}
