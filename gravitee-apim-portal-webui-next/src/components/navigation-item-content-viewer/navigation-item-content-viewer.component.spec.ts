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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NavigationItemContentViewerComponent } from './navigation-item-content-viewer.component';
import { NavigationItemContentViewerHarness } from './navigation-item-content-viewer.harness';
import { PortalPageContent } from '../../entities/portal-navigation/portal-page-content';

interface initData {
  pageContent: PortalPageContent | null;
}

describe('NavigationItemContentViewerComponent', () => {
  let fixture: ComponentFixture<NavigationItemContentViewerComponent>;
  let harness: NavigationItemContentViewerHarness;

  const init = async (data: initData) => {
    await TestBed.configureTestingModule({
      imports: [NavigationItemContentViewerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationItemContentViewerComponent);
    fixture.componentRef.setInput('pageContent', data.pageContent);

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, NavigationItemContentViewerHarness);
    fixture.detectChanges();
  };

  describe('if page content is GRAVITEE_MARKDOWN', () => {
    const pageContent: PortalPageContent = {
      type: 'GRAVITEE_MARKDOWN',
      content: '# Hello World\nThis is a test markdown content.',
    };
    beforeEach(async () => {
      await init({ pageContent });
    });
    it('should render markdown viewer', async () => {
      const markdownViewer = await harness.getGMDViewer();
      expect(markdownViewer).not.toBeNull();

      expect(await markdownViewer?.getRenderedHtml()).toContain('<h1 id="hello-world">Hello World</h1>');
    });
  });

  describe('if page content is empty', () => {
    beforeEach(async () => {
      await init({ pageContent: null });
    });

    it('should not render markdown viewer', async () => {
      const markdownViewer = await harness.getGMDViewer();
      expect(markdownViewer).toBeNull();
    });

    it('should display no content message', async () => {
      expect(await harness.isShowingEmptyState()).toBe(true);
    });
  });
});
