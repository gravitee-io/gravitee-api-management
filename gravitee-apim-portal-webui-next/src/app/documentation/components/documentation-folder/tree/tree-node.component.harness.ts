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

import { DivHarness } from '../../../../../testing/div.harness';

export class TreeNodeComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-tree-node';

  readonly contentContainer = this.locatorForOptional(DivHarness.with({ selector: '.tree__row' }));

  readonly childrenContainer = this.locatorForOptional(DivHarness.with({ selector: '.tree__children' }));

  async getText() {
    const container = await this.contentContainer();
    const label = await container?.childLocatorForOptional('.tree__label')();
    if (label) {
      return label?.text();
    } else {
      const link = await container?.childLocatorForOptional('.tree__link')();
      const child = await link?.getProperty<ChildNode>('firstChild');
      return child?.textContent;
    }
  }

  async getChildren() {
    const container = await this.childrenContainer();
    const children = await container?.childLocatorForAll(TreeNodeComponentHarness)();
    return children ?? [];
  }

  async isTopLevel() {
    const container = await this.contentContainer();
    const host = await container?.host();
    const level = await host?.getAttribute('aria-level');
    return level === '1';
  }

  async isSelected() {
    const container = await this.contentContainer();
    const host = await container?.host();
    return host?.hasClass('selected');
  }
}
