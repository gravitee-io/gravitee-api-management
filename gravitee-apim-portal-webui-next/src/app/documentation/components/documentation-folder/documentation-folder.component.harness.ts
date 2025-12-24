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

import { GraviteeMarkdownViewerHarness } from '@gravitee/gravitee-markdown';

import { BreadcrumbsComponentHarness } from './breadcrumb/breadcrumbs.component.harness';
import { SidenavToggleButtonComponentHarness } from './sidenav-toggle-button/sidenav-toggle-button.component.harness';
import { TreeComponentHarness } from './tree/tree.component.harness';
import { DivHarness } from '../../../../testing/div.harness';

export class DocumentationFolderComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-documentation-folder';

  private readonly getSidenavHarness = this.locatorForOptional(DivHarness.with({ selector: '.documentation-folder__sidenav' }));

  private readonly getSidenavToggleButtonHarness = this.locatorForOptional(SidenavToggleButtonComponentHarness);

  private readonly getTree = this.locatorForOptional(TreeComponentHarness);

  private readonly getBreadcrumbsHarness = this.locatorForOptional(BreadcrumbsComponentHarness);

  private readonly getContentEmptyStateHarness = this.locatorForOptional(
    DivHarness.with({ selector: '.documentation-folder__container .empty-state' }),
  );
  private readonly getGraviteeMarkdownViewer = this.locatorForOptional(GraviteeMarkdownViewerHarness);

  async getSidenav(): Promise<DivHarness | null> {
    return this.getSidenavHarness();
  }

  async getSidenavCollapsedState(): Promise<boolean | null> {
    return this.getSidenav()
      .then(sidenav => sidenav?.host())
      .then(host => host?.hasClass('collapsed') ?? null);
  }

  async getSidenavEmptyState(): Promise<DivHarness | null> {
    return this.getSidenavHarness().then(sidenav =>
      sidenav ? sidenav.childLocatorForOptional(DivHarness.with({ selector: '.documentation-folder__sidenav__empty-state' }))() : null,
    );
  }

  async getSidenavToggleButton(): Promise<SidenavToggleButtonComponentHarness | null> {
    return this.getSidenavToggleButtonHarness();
  }

  async getTreeHarness(): Promise<TreeComponentHarness | null> {
    return this.getTree();
  }

  async getBreadcrumbs(): Promise<BreadcrumbsComponentHarness | null> {
    return this.getBreadcrumbsHarness();
  }

  async getContentEmptyState(): Promise<DivHarness | null> {
    return this.getContentEmptyStateHarness();
  }

  async getGmdViewer(): Promise<GraviteeMarkdownViewerHarness | null> {
    return this.getGraviteeMarkdownViewer();
  }
}
