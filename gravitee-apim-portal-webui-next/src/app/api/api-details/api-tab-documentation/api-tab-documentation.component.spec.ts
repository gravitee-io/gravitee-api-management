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

import { ApiTabDocumentationComponent } from './api-tab-documentation.component';
import { PageTreeHarness } from '../../../../components/page-tree/page-tree.harness';
import { fakePage } from '../../../../entities/page/page.fixtures';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('ApiTabDocumentationComponent', () => {
  let component: ApiTabDocumentationComponent;
  let fixture: ComponentFixture<ApiTabDocumentationComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiTabDocumentationComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabDocumentationComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.apiId = 'api-id';
  });

  it('should change shown page when different page clicked', async () => {
    component.pages = [fakePage({ id: 'page-1', name: 'Page 1' }), fakePage({ id: 'page-2', name: 'Page 2' })];
    fixture.detectChanges();

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
});
