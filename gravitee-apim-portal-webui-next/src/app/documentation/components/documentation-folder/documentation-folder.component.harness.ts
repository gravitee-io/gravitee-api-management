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

import { GraviteeMarkdownViewerHarness } from '@gravitee/gravitee-markdown';

import { TreeComponentHarness } from './tree/tree.component.harness';
import { BreadcrumbsComponentHarness } from '../../../../components/breadcrumbs/breadcrumbs.component.harness';
import { NavigationItemContentViewerHarness } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.harness';
import { SidenavLayoutComponentHarness } from '../../../../components/sidenav-layout/sidenav-layout.component.harness';
import { SidenavToggleButtonComponentHarness } from '../../../../components/sidenav-toggle-button/sidenav-toggle-button.component.harness';
import { DivHarness } from '../../../../testing/div.harness';

export class DocumentationFolderComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-documentation-folder';

  private readonly getSidenavLayoutHarness = this.locatorFor(SidenavLayoutComponentHarness);
  private readonly getTree = this.locatorForOptional(TreeComponentHarness);
  private readonly getBreadcrumbsHarness = this.locatorForOptional(BreadcrumbsComponentHarness);
  private readonly getSidenavEmptyStateHarness = this.locatorForOptional(
    DivHarness.with({ selector: '.documentation-folder__sidenav__empty-state' }),
  );
  private readonly getNavigationItemContentViewerHarness = this.locatorForOptional(NavigationItemContentViewerHarness);
  private readonly getGraviteeMarkdownViewer = this.locatorForOptional(GraviteeMarkdownViewerHarness);
  private readonly getSubscribeMatButton = this.locatorForOptional(MatButtonHarness.with({ text: 'Subscribe' }));

  async getSidenavToggleButton(): Promise<SidenavToggleButtonComponentHarness | null> {
    const sidenav = await this.getSidenavLayoutHarness();
    return sidenav.getSidenavToggleButton();
  }

  async getSidenavEmptyState(): Promise<DivHarness | null> {
    return this.getSidenavEmptyStateHarness();
  }

  async getTreeHarness(): Promise<TreeComponentHarness | null> {
    return this.getTree();
  }

  async getBreadcrumbs(): Promise<BreadcrumbsComponentHarness | null> {
    return this.getBreadcrumbsHarness();
  }

  async getContentEmptyState(): Promise<DivHarness | null> {
    const harness = await this.getNavigationItemContentViewerHarness();
    return harness ? harness.getEmptyState() : null;
  }

  async getGmdViewer(): Promise<GraviteeMarkdownViewerHarness | null> {
    return this.getGraviteeMarkdownViewer();
  }

  async getSubscribeButton(): Promise<MatButtonHarness | null> {
    return this.getSubscribeMatButton();
  }
}
