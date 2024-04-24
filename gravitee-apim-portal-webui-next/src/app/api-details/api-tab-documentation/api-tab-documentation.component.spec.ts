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
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiTabDocumentationComponent } from './api-tab-documentation.component';
import { PageTreeHarness } from '../../../components/page-tree/page-tree.harness';
import { fakePage, fakePagesResponse } from '../../../entities/page/page.fixtures';
import { PagesResponse } from '../../../entities/page/pages-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('ApiTabDocumentationComponent', () => {
  let component: ApiTabDocumentationComponent;
  let fixture: ComponentFixture<ApiTabDocumentationComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiTabDocumentationComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({ page: undefined }) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabDocumentationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.apiId = 'api-id';
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should change shown page when different page clicked', async () => {
    expectPageList(fakePagesResponse({ data: [fakePage({ id: 'page-1', name: 'Page 1' }), fakePage({ id: 'page-2', name: 'Page 2' })] }));

    const pageTree = await harnessLoader.getHarnessOrNull(PageTreeHarness);
    expect(pageTree).toBeTruthy();
    expect(await pageTree?.getActivePageName()).toEqual('Page 1');
    component.selectedPageData$.subscribe(data => {
      expect(data.result?.id).toEqual('page-1');
    });

    await pageTree?.clickPage('Page 2');
    expect(await pageTree?.getActivePageName()).toEqual('Page 2');
    component.selectedPageData$.subscribe(data => {
      expect(data.result?.id).toEqual('page-2');
    });
  });

  function expectPageList(pagesResponse: PagesResponse = fakePagesResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages?homepage=false&page=1&size=-1`).flush(pagesResponse);
  }
});
