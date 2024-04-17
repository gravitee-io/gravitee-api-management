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
import { MatTabHarness } from '@angular/material/tabs/testing';

import { ApiDetailsComponent } from './api-details.component';
import { fakeApi } from '../../entities/api/api.fixtures';
import { fakePagesResponse } from '../../entities/page/page.fixtures';
import { PagesResponse } from '../../entities/page/pages-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('ApiDetailsComponent', () => {
  let component: ApiDetailsComponent;
  let fixture: ComponentFixture<ApiDetailsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiDetailsComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDetailsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.apiId = 'api-id';
    fixture.detectChanges();
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id`).flush(fakeApi({ id: 'api-id' }));
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.match('assets/images/lightbulb_24px.svg');
    httpTestingController.verify();
  });

  it('should disable Documentation tab when pages empty', async () => {
    expectPageList(fakePagesResponse({ data: [] }));
    const documentationTab = await harnessLoader.getHarness(MatTabHarness.with({ label: 'Documentation' }));
    expect(await documentationTab.isDisabled()).toEqual(true);
  });

  it('should have active Documentation tab if pages exist', async () => {
    expectPageList(fakePagesResponse());
    const documentationTab = await harnessLoader.getHarness(MatTabHarness.with({ label: 'Documentation' }));
    expect(await documentationTab.isDisabled()).toEqual(false);
  });

  function expectPageList(pagesResponse: PagesResponse = fakePagesResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/api-id/pages?homepage=false&page=1&size=-1`).flush(pagesResponse);
  }
});
