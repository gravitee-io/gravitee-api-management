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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';

import { DivHarness } from '../../../../../testing/div.harness';

export class TreeRowHarness extends ComponentHarness {
  static readonly hostSelector = '.tree__row';

  public static with(options: BaseHarnessFilters & { text?: string | RegExp }): HarnessPredicate<TreeRowHarness> {
    return new HarnessPredicate(TreeRowHarness, options).addOption('text', options.text, (harness, text) =>
      HarnessPredicate.stringMatches(harness.getText(), text),
    );
  }

  async getText() {
    const label = await this.locatorForOptional('.tree__label')();
    if (label) {
      return label.text();
    }

    const host = await this.host();
    return host.text();
  }
  async isExpanded(): Promise<boolean> {
    const icon = await this.locatorForOptional('.tree__icon')();
    return icon ? icon.hasClass('expanded') : false;
  }
}

export class TreeNodeComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-tree-node';

  readonly contentContainer = this.locatorForOptional(TreeRowHarness);

  readonly childrenContainer = this.locatorForOptional(DivHarness.with({ selector: '.tree__children' }));

  async getText() {
    const container = await this.contentContainer();
    return container?.getText();
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
    const selected = await host?.getAttribute('aria-selected');
    return selected === 'true';
  }
}
