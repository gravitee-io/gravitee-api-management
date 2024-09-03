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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageAsciidocHarness } from './page-asciidoc/page-asciidoc.harness';
import { PageAsyncApiHarness } from './page-async-api/page-async-api.harness';
import { PageMarkdownHarness } from './page-markdown/page-markdown.harness';
import { PageRedocHarness } from './page-redoc/page-redoc.harness';
import { PageSwaggerHarness } from './page-swagger/page-swagger.harness';
import { PageComponent } from './page.component';
import { fakePage } from '../../entities/page/page.fixtures';
import { RedocService } from '../../services/redoc.service';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('PageComponent', () => {
  let component: PageComponent;
  let fixture: ComponentFixture<PageComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageComponent, AppTestingModule],
      providers: [
        {
          provide: RedocService,
          useValue: { init: (_content: string | undefined, _options: unknown, _el: unknown) => {} },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PageComponent);
    harnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    component.apiId = 'api-id';
    component.pages = [];
  });

  describe('swagger', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'SWAGGER' });
      fixture.detectChanges();
    });

    it('should show swagger content', async () => {
      const swagger = await harnessLoader.getHarnessOrNull(PageSwaggerHarness);
      expect(swagger).toBeTruthy();
      expect(await swagger?.getSwagger()).toBeTruthy();
    });
  });

  describe('redoc', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'SWAGGER', configuration: { viewer: 'Redoc' } });
      fixture.detectChanges();
    });

    it('should show redoc content', async () => {
      const redoc = await harnessLoader.getHarnessOrNull(PageRedocHarness);
      expect(redoc).toBeTruthy();
      expect(await redoc?.getRedoc()).toBeTruthy();
    });
  });

  describe('markdown', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'MARKDOWN' });
      fixture.detectChanges();
    });

    it('should show markdown content', async () => {
      const markdown = await harnessLoader.getHarnessOrNull(PageMarkdownHarness);
      expect(markdown).toBeTruthy();
    });
  });

  describe('asciidoc', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'ASCIIDOC' });
      fixture.detectChanges();
    });

    it('should show asciidoc content', async () => {
      const asciidoc = await harnessLoader.getHarnessOrNull(PageAsciidocHarness);
      expect(asciidoc).toBeTruthy();
    });
  });

  describe('async api', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'ASYNCAPI' });
      fixture.detectChanges();
    });

    it('should show async api content', async () => {
      const asyncApi = await harnessLoader.getHarnessOrNull(PageAsyncApiHarness);
      expect(asyncApi).toBeTruthy();
      expect(asyncApi?.schemaIsShown()).toBeTruthy();
    });
  });
});
