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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { ApiDocumentationPagesComponent } from './api-documentation-pages.component';
import { ApiDocumentationPagesHarness } from './api-documentation-pages.harness';
import { Page } from '../../../../entities/page/page';
import { fakePage, fakePagesResponse } from '../../../../entities/page/page.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApiDocumentationPagesComponent', () => {
  const API_ID = 'api-id';
  const PAGES_URL = `${TESTING_BASE_URL}/apis/${API_ID}/pages?homepage=false&page=1&size=-1`;
  let fixture: ComponentFixture<ApiDocumentationPagesComponent>;
  let harness: ApiDocumentationPagesHarness;
  let httpTestingController: HttpTestingController;

  const markdownPage = fakePage({ id: 'page-md', name: 'Getting started', type: 'MARKDOWN', order: 1 });
  const swaggerPage = fakePage({ id: 'page-oas', name: 'Specification', type: 'SWAGGER', order: 0 });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiDocumentationPagesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL, configuration: {} } },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ApiDocumentationPagesComponent);
    fixture.componentRef.setInput('apiId', API_ID);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  // Requests are flushed before the harness is used: a pending rxResource request keeps the fixture unstable.
  const settle = async () => {
    await new Promise<void>(resolve => setTimeout(resolve, 0));
    fixture.detectChanges();
  };

  const flushPages = async (pages: Page[]) => {
    fixture.detectChanges();
    httpTestingController.expectOne(PAGES_URL).flush(fakePagesResponse({ data: pages }));
    await settle();
  };

  const flushPageContent = async (page: Page) => {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}/pages/${page.id}?include=content`).flush(page);
    await settle();
  };

  const createHarness = async () => {
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationPagesHarness);
  };

  it('should show the first published API page and list every page', async () => {
    await flushPages([markdownPage, swaggerPage]);
    await flushPageContent(swaggerPage);
    await createHarness();

    const pageTree = await harness.getPageTree();
    expect(await pageTree?.displayedItems()).toEqual(['Specification', 'Getting started']);
    expect(await pageTree?.getActivePageName()).toEqual('Specification');
    expect(await harness.getDisplayedPageType()).toEqual('SWAGGER');
  });

  it('should show another API page when it is selected', async () => {
    await flushPages([markdownPage, swaggerPage]);
    await flushPageContent(swaggerPage);
    await createHarness();

    const pageTree = await harness.getPageTree();
    // The harness click waits for stability, which only comes once the page content request is flushed.
    const click = pageTree!.clickPage('Getting started');
    const markdownPageUrl = `${TESTING_BASE_URL}/apis/${API_ID}/pages/${markdownPage.id}?include=content`;
    let requests = httpTestingController.match(markdownPageUrl);
    for (let attempt = 0; requests.length === 0 && attempt < 10; attempt++) {
      await settle();
      requests = httpTestingController.match(markdownPageUrl);
    }
    expect(requests).toHaveLength(1);
    requests[0].flush(markdownPage);
    await settle();
    await click;

    expect(await pageTree!.getActivePageName()).toEqual('Getting started');
    expect(await harness.getDisplayedPageType()).toEqual('MARKDOWN');
  });

  it('should not show the page list when the API has a single page', async () => {
    await flushPages([markdownPage]);
    await flushPageContent(markdownPage);
    await createHarness();

    expect(await harness.getPageTree()).toBeNull();
    expect(await harness.getDisplayedPageType()).toEqual('MARKDOWN');
  });

  it('should show an empty state when the API has no published page', async () => {
    await flushPages([]);
    await createHarness();

    expect(await harness.getEmptyStateText()).toEqual('Documentation for this API is missing.');
    expect(await harness.getDisplayedPageType()).toBeNull();
  });

  it('should show an error when the API pages cannot be loaded', async () => {
    fixture.detectChanges();
    httpTestingController.expectOne(PAGES_URL).flush({}, { status: 500, statusText: 'Server error' });
    await settle();
    await createHarness();

    expect(await harness.getErrorText()).toEqual('An error occurred while loading the documentation of this API.');
  });
});
