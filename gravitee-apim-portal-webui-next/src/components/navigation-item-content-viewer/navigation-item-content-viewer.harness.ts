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

import { GraviteeMarkdownViewerHarness } from '@gravitee/gravitee-markdown';

import { DivHarness } from '../../testing/div.harness';

export class NavigationItemContentViewerHarness extends ComponentHarness {
  public static hostSelector = 'app-navigation-item-content-viewer';

  private locateGMDViewer = this.locatorForOptional(GraviteeMarkdownViewerHarness);
  private locateEmptyState = this.locatorForOptional(DivHarness.with({ selector: '.empty-state' }));

  public async isShowingMarkdownContent(): Promise<boolean> {
    const gmdViewer = await this.locateGMDViewer();
    return gmdViewer !== null;
  }

  public async getGMDViewer(): Promise<GraviteeMarkdownViewerHarness | null> {
    return this.locateGMDViewer();
  }

  public async getEmptyState(): Promise<DivHarness | null> {
    return await this.locateEmptyState();
  }

  public async isShowingEmptyState(): Promise<boolean> {
    const emptyState = await this.getEmptyState();
    return emptyState !== null;
  }
}
