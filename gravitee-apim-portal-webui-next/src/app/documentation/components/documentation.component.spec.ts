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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideRouter, Router } from '@angular/router';

import { DocumentationComponent } from './documentation.component';
import { DocumentationComponentHarness } from './documentation.component.harness';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { fakePortalNavigationFolder, fakePortalNavigationPage } from '../../../entities/portal-navigation/portal-navigation-item.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';
import { NotFoundComponent } from '../../not-found/not-found.component';

describe('DocumentationComponent', () => {
  let fixture: ComponentFixture<DocumentationComponent>;
  let harness: DocumentationComponentHarness;
  let routerSpy: jest.SpyInstance;
  let httpTestingController: HttpTestingController;

  const init = async (navItem: PortalNavigationItem | null) => {
    await TestBed.configureTestingModule({
      imports: [DocumentationComponent, MatIconTestingModule, AppTestingModule],
      providers: [provideRouter([{ path: '404', component: NotFoundComponent }])],
    }).compileComponents();

    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

    fixture = TestBed.createComponent(DocumentationComponent);
    fixture.componentRef.setInput('navItem', navItem);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationComponentHarness);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('initial load', () => {
    it('should display folder', async () => {
      await init(fakePortalNavigationFolder({ id: 'folder1' }));

      expectGetNavigationItemsRequest('folder1', []);

      const folder = await harness.getFolder();
      expect(folder).not.toBeNull();

      const page = await harness.getPage();
      expect(await page?.isShowingEmptyState()).toEqual(true);
    });

    it('should redirect to 404', async () => {
      await init(null);

      expect(routerSpy).toHaveBeenCalledWith(['/404']);
    });

    it('should display page', async () => {
      await init(fakePortalNavigationPage({ id: 'page1' }));

      expectPageContentRequest('page1', 'This is the page content');

      const page = await harness.getPage();
      expect(page).not.toBeNull();
      expect(await page!.isShowingMarkdownContent()).toBe(true);
    });
  });

  function expectPageContentRequest(pageId: string, content: string) {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-navigation-items/${pageId}/content`);
    expect(req.request.method).toEqual('GET');
    req.flush({ content, type: 'GRAVITEE_MARKDOWN' });
  }

  function expectGetNavigationItemsRequest(parentId: string, response: PortalNavigationItem[]) {
    const req = httpTestingController.expectOne(
      `${TESTING_BASE_URL}/portal-navigation-items?parentId=${parentId}&area=TOP_NAVBAR&loadChildren=true`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  }
});
