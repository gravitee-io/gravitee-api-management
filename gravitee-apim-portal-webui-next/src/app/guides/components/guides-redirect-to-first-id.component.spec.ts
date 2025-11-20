/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { GuidesRedirectToFirstIdComponent } from './guides-redirect-to-first-id.component';
import { fakePage, fakePagesResponse } from '../../../entities/page/page.fixtures';
import { PagesResponse } from '../../../entities/page/pages-response';
import { PageService } from '../../../services/page.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('GuidesRedirectToFirstIdComponent', () => {
  let fixture: ComponentFixture<GuidesRedirectToFirstIdComponent>;
  let httpTestingController: HttpTestingController;
  let router: Router;
  let activatedRoute: ActivatedRoute;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuidesRedirectToFirstIdComponent, AppTestingModule],
      providers: [PageService],
    }).compileComponents();

    fixture = TestBed.createComponent(GuidesRedirectToFirstIdComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    activatedRoute = TestBed.inject(ActivatedRoute);

    jest.spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('ngOnInit', () => {
    it('should redirect to the first page when it is a direct page (not in a folder)', () => {
      const firstPage = fakePage({ id: 'first-page', name: 'First Page', type: 'MARKDOWN' });
      const secondPage = fakePage({ id: 'second-page', name: 'Second Page', type: 'MARKDOWN' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [firstPage, secondPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'first-page'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should redirect to the first non-folder, non-link page', () => {
      const folder = fakePage({ id: 'folder-id', name: 'Folder', type: 'FOLDER' });
      const pageInFolder = fakePage({ id: 'page-in-folder', name: 'Page In Folder', type: 'MARKDOWN', parent: 'folder-id' });
      const regularPage = fakePage({ id: 'regular-page', name: 'Regular Page', type: 'MARKDOWN' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [folder, pageInFolder, regularPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'page-in-folder'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should skip LINK type pages and redirect to the first valid page', () => {
      const linkPage = fakePage({ id: 'link-page', name: 'Link Page', type: 'LINK' });
      const markdownPage = fakePage({ id: 'markdown-page', name: 'Markdown Page', type: 'MARKDOWN' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [linkPage, markdownPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'markdown-page'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should skip folders and find the first non-folder page', () => {
      const rootFolder = fakePage({ id: 'root-folder', name: 'Root Folder', type: 'FOLDER' });
      const nestedFolder = fakePage({ id: 'nested-folder', name: 'Nested Folder', type: 'FOLDER', parent: 'root-folder' });
      const nestedPage = fakePage({ id: 'nested-page', name: 'Nested Page', type: 'MARKDOWN', parent: 'nested-folder' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [rootFolder, nestedFolder, nestedPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'nested-page'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should redirect to the first non-folder, non-link page in the list', () => {
      const page1 = fakePage({ id: 'page-1', name: 'Page 1', type: 'ASCIIDOC', order: 2 });
      const page2 = fakePage({ id: 'page-2', name: 'Page 2', type: 'MARKDOWN', order: 1 });
      const page3 = fakePage({ id: 'page-3', name: 'Page 3', type: 'SWAGGER', order: 3 });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [page1, page2, page3] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'page-1'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should not redirect when there are no pages', () => {
      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [] }));

      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should not redirect when all pages are LINK type', () => {
      const linkPage1 = fakePage({ id: 'link-1', name: 'Link 1', type: 'LINK' });
      const linkPage2 = fakePage({ id: 'link-2', name: 'Link 2', type: 'LINK' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [linkPage1, linkPage2] }));

      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should not redirect when there are only folders', () => {
      const emptyFolder = fakePage({ id: 'empty-folder', name: 'Empty Folder', type: 'FOLDER' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [emptyFolder] }));

      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should skip folders and links to find the first valid page', () => {
      const folder = fakePage({ id: 'folder-1', name: 'Folder 1', type: 'FOLDER' });
      const link = fakePage({ id: 'link-1', name: 'Link 1', type: 'LINK' });
      const asyncApiPage = fakePage({ id: 'async-page', name: 'Async API Page', type: 'ASYNCAPI', parent: 'folder-1' });
      const swaggerPage = fakePage({ id: 'swagger-page', name: 'Swagger Page', type: 'SWAGGER' });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [folder, link, asyncApiPage, swaggerPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'async-page'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });

    it('should redirect to the first non-folder, non-link page regardless of order', () => {
      const folder = fakePage({ id: 'folder-id', name: 'Documentation', type: 'FOLDER', order: 1 });
      const pageInFolder = fakePage({ id: 'page-in-folder', name: 'Getting Started', type: 'MARKDOWN', parent: 'folder-id', order: 1 });
      const rootPage = fakePage({ id: 'root-page', name: 'Root Page', type: 'MARKDOWN', order: 2 });

      fixture.detectChanges();

      expectGetPages(fakePagesResponse({ data: [folder, pageInFolder, rootPage] }));

      expect(router.navigate).toHaveBeenCalledWith(['.', 'page-in-folder'], {
        relativeTo: activatedRoute,
        replaceUrl: true,
      });
    });
  });

  function expectGetPages(pagesResponse: PagesResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/pages?page=1&size=-1`).flush(pagesResponse);
    fixture.detectChanges();
  }
});
