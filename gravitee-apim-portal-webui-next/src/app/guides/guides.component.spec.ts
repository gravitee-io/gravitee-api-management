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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { GuidesComponent } from './guides.component';
import { PageTreeHarness } from '../../components/page-tree/page-tree.harness';
import { fakePage, fakePagesResponse } from '../../entities/page/page.fixtures';
import { PagesResponse } from '../../entities/page/pages-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('GuidesComponent', () => {
  let fixture: ComponentFixture<GuidesComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    const paramMap = new BehaviorSubject(new Map());
    const url = new BehaviorSubject([]);

    await TestBed.configureTestingModule({
      imports: [GuidesComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            url: url.asObservable(),
            firstChild: {
              paramMap: paramMap.asObservable(),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GuidesComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('No pages', () => {
    it('should show empty message', async () => {
      expectGetPages(fakePagesResponse({ data: [] }));
      fixture.detectChanges();

      const emptyContainer = await harnessLoader.getHarness(MatCardHarness);
      expect(await emptyContainer.getText()).toContain('Sorry, there are no guides listed yet.');
    });
  });

  describe('With pages', () => {
    it('should not show LINKS', async () => {
      expectGetPages(
        fakePagesResponse({
          data: [
            fakePage({ id: 'link-id', name: 'a link', type: 'LINK' }),
            fakePage({ id: 'valid-page', name: 'valid page', type: 'MARKDOWN' }),
          ],
        }),
      );

      const tree = await harnessLoader.getHarness(PageTreeHarness);
      const displayedItems = await tree.displayedItems();
      expect(displayedItems).toHaveLength(1);
      expect(displayedItems).toEqual(['valid page']);
    });

    it('should not show empty folder', async () => {
      expectGetPages(
        fakePagesResponse({
          data: [
            fakePage({ id: 'empty-folder', name: 'an empty folder', type: 'FOLDER' }),
            fakePage({ id: 'valid-folder', name: 'a valid folder', type: 'FOLDER' }),
            fakePage({ id: 'valid-page', name: 'valid page', type: 'MARKDOWN', parent: 'valid-folder' }),
          ],
        }),
      );

      const tree = await harnessLoader.getHarness(PageTreeHarness);
      const displayedItems = await tree.displayedItems();
      expect(displayedItems).toHaveLength(1);
      expect(displayedItems).toEqual(['a valid folder']);
    });

    it('should not show page with parentId defined but invalid', async () => {
      expectGetPages(
        fakePagesResponse({
          data: [
            fakePage({ id: 'invalid-folder', name: 'invalid folder', type: 'FOLDER', parent: 'no-parent' }),
            fakePage({ id: 'invalid-page', name: 'invalid page', type: 'MARKDOWN', parent: 'invalid-folder' }),
            fakePage({ id: 'valid-page', name: 'valid page', type: 'MARKDOWN' }),
          ],
        }),
      );

      const tree = await harnessLoader.getHarness(PageTreeHarness);
      const displayedItems = await tree.displayedItems();
      expect(displayedItems).toHaveLength(1);
      expect(displayedItems).toEqual(['valid page']);
    });

    it('should show page', async () => {
      expectGetPages(fakePagesResponse({ data: [fakePage({ id: 'valid-page', name: 'valid page', type: 'MARKDOWN' })] }));

      const tree = await harnessLoader.getHarness(PageTreeHarness);
      const displayedItems = await tree.displayedItems();
      expect(displayedItems).toHaveLength(1);
      expect(displayedItems).toEqual(['valid page']);
    });

    it('should not show folder + inner page', async () => {
      expectGetPages(
        fakePagesResponse({
          data: [
            fakePage({ id: 'valid-folder', name: 'valid folder', type: 'FOLDER', content: undefined }),
            fakePage({ id: 'valid-page', name: 'valid page', type: 'MARKDOWN', parent: 'valid-folder' }),
          ],
        }),
      );

      const tree = await harnessLoader.getHarness(PageTreeHarness);
      const displayedItems = await tree.displayedItems();
      expect(displayedItems).toHaveLength(1);
      expect(displayedItems).toEqual(['valid folder']);
    });
  });

  function expectGetPages(pagesResponse: PagesResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/pages?page=1&size=-1`).flush(pagesResponse);
    fixture.detectChanges();
  }
});
