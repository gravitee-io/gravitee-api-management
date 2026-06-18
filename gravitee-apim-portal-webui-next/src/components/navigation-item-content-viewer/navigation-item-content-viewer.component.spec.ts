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
import { ComponentFixture, TestBed, TestModuleMetadata } from '@angular/core/testing';
import SwaggerUI from 'swagger-ui';

import { NavigationItemContentViewerComponent } from './navigation-item-content-viewer.component';
import { NavigationItemContentViewerHarness } from './navigation-item-content-viewer.harness';
import { ViewerEnum } from '../../entities/page/page-configuration';
import { PortalPageContent } from '../../entities/portal-navigation/portal-page-content';
import { RedocService } from '../../services/redoc.service';
import { AppTestingModule } from '../../testing/app-testing.module';

interface InitData {
  pageContent: PortalPageContent | null;
  imports?: TestModuleMetadata['imports'];
  providers?: TestModuleMetadata['providers'];
  clearSwaggerUI?: boolean;
}

describe('NavigationItemContentViewerComponent', () => {
  let fixture: ComponentFixture<NavigationItemContentViewerComponent>;
  let harness: NavigationItemContentViewerHarness;

  const init = async ({ pageContent, imports = [], providers = [], clearSwaggerUI = false }: InitData) => {
    await TestBed.configureTestingModule({
      imports: [NavigationItemContentViewerComponent, ...imports],
      providers,
    }).compileComponents();

    if (clearSwaggerUI) {
      jest.mocked(SwaggerUI).mockClear();
    }

    fixture = TestBed.createComponent(NavigationItemContentViewerComponent);
    fixture.componentRef.setInput('pageContent', pageContent);

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

  describe('if page content is OPENAPI', () => {
    const pageContent: PortalPageContent = {
      type: 'OPENAPI',
      content: 'openapi: 3.0.0\ninfo:\n  title: Test\n  version: 1.0.0',
      configuration: { viewer: ViewerEnum.Redoc, try_it_url: 'https://try-it.example.com' },
    };
    beforeEach(async () => {
      await init({
        pageContent,
        providers: [
          {
            provide: RedocService,
            useValue: { init: (_content: string | undefined, _options: unknown, _el: unknown) => {} },
          },
        ],
      });
    });
    it('should render redoc viewer', async () => {
      const redocViewer = await harness.getRedocViewer();
      expect(redocViewer).not.toBeNull();
      expect(await harness.isShowingRedocContent()).toBe(true);
    });
    it('should read Redoc try it URL from snake_case configuration', () => {
      expect(fixture.componentInstance.tryItUrl()).toBe('https://try-it.example.com');
    });
    it('should not pass Redoc try it URL when backend resolved server URLs are enabled', () => {
      fixture.componentRef.setInput('pageContent', {
        ...pageContent,
        configuration: { ...pageContent.configuration, context_path_as_server_path: true },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance.tryItUrl()).toBeUndefined();
    });
    it('should not render markdown viewer', async () => {
      const markdownViewer = await harness.getGMDViewer();
      expect(markdownViewer).toBeNull();
    });
  });

  describe('if page content is ASYNCAPI', () => {
    const pageContent: PortalPageContent = {
      type: 'ASYNCAPI',
      content: 'asyncapi: 2.0.0\ninfo:\n  title: Test\n  version: 1.0.0',
    };
    beforeEach(async () => {
      await init({ pageContent });
    });
    it('should render async api viewer', async () => {
      const asyncApiViewer = await harness.getAsyncApiViewer();
      expect(asyncApiViewer).not.toBeNull();
    });
    it('should not render markdown viewer', async () => {
      const markdownViewer = await harness.getGMDViewer();
      expect(markdownViewer).toBeNull();
    });
    it('should not render redoc viewer', async () => {
      expect(await harness.isShowingRedocContent()).toBe(false);
    });
  });

  describe('if page content is OPENAPI with Swagger', () => {
    const pageContent: PortalPageContent = {
      type: 'OPENAPI',
      content: 'openapi: 3.0.0\ninfo:\n  title: Test\n  version: 1.0.0',
      configuration: { viewer: ViewerEnum.Swagger },
    };

    beforeEach(async () => {
      await init({ pageContent, imports: [AppTestingModule], clearSwaggerUI: true });
    });

    it('should not reinitialize Swagger UI on unchanged change detection cycles', () => {
      expect(SwaggerUI).toHaveBeenCalledTimes(1);

      fixture.detectChanges();
      fixture.detectChanges();

      expect(SwaggerUI).toHaveBeenCalledTimes(1);
    });

    it('should render swagger viewer', async () => {
      const swaggerViewer = await harness.getSwaggerViewer();
      expect(swaggerViewer).not.toBeNull();
    });

    it('should pass Swagger try it URL when backend server resolution is disabled', () => {
      fixture.componentRef.setInput('pageContent', {
        ...pageContent,
        configuration: { viewer: ViewerEnum.Swagger, try_it_url: 'https://try-it.example.com' },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance.swaggerPage()?.configuration?.try_it_url).toBe('https://try-it.example.com');
    });

    it('should not pass Swagger try it URL when backend resolved server URLs are enabled', () => {
      fixture.componentRef.setInput('pageContent', {
        ...pageContent,
        configuration: { viewer: ViewerEnum.Swagger, try_it_url: 'https://try-it.example.com', entrypoints_as_servers: true },
      });
      fixture.detectChanges();

      expect(fixture.componentInstance.swaggerPage()?.configuration?.try_it_url).toBeUndefined();
    });

    it('should not render redoc viewer', async () => {
      expect(await harness.isShowingRedocContent()).toBe(false);
    });
  });

  describe('if page content is OPENAPI with no viewer set', () => {
    const pageContent: PortalPageContent = {
      type: 'OPENAPI',
      content: 'openapi: 3.0.0\ninfo:\n  title: Test\n  version: 1.0.0',
    };

    beforeEach(async () => {
      await init({ pageContent, imports: [AppTestingModule], clearSwaggerUI: true });
    });

    it('should render swagger viewer', async () => {
      const swaggerViewer = await harness.getSwaggerViewer();
      expect(swaggerViewer).not.toBeNull();
    });

    it('should not render redoc viewer', async () => {
      expect(await harness.isShowingRedocContent()).toBe(false);
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
