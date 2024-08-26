/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MatTreeNodeHarness } from '@angular/material/tree/testing';

export class PageTreeHarness extends ComponentHarness {
  public static hostSelector = 'app-page-tree';
  protected locateActiveNode = this.locatorFor(MatTreeNodeHarness.with({ selector: '.active' }));
  protected locateNodes = this.locatorForAll(MatTreeNodeHarness);

  public async getActivePageName(): Promise<string> {
    const activeNode = await this.locateActiveNode();
    return await activeNode.getText();
  }

  public async clickPage(pageName: string): Promise<void> {
    return await this.locateNode(pageName)
      .then(node => node.host())
      .then(host => host.click());
  }

  public async displayedItems(): Promise<string[]> {
    return await this.locateNodes().then(nodes => {
      return Promise.all(nodes.map(async n => await n.getText()));
    });
  }

  protected locateNode = (text: string) => this.locatorFor(MatTreeNodeHarness.with({ text }))();
}
