/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTabNavBarHarness } from '@angular/material/tabs/testing';
import { RouterModule } from '@angular/router';

import { HomeLayoutComponent } from './home-layout.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

describe('HomeLayoutComponent', () => {
  let fixture: ComponentFixture<HomeLayoutComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [HomeLayoutComponent],
      imports: [NoopAnimationsModule, GioTestingModule, RouterModule, MatTabsModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeLayoutComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display tabs', async () => {
    const tabs = await loader.getHarness(MatTabNavBarHarness);
    const links = await tabs.getLinks();
    expect(await links[0].getLabel()).toEqual('Overview');
    expect(await links[1].getLabel()).toEqual('API Health Check');
    expect(await links[2].getLabel()).toEqual('Tasks');
    expect(await links[3].getLabel()).toEqual('Broadcasts');

    // Change Tasks label when tasks are loaded
    expectGetTasks();
    expect(await links[2].getLabel()).toEqual('My Tasks 42');
  });

  function expectGetTasks() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/user/tasks`, method: 'GET' }).flush({
      data: [],
      metadata: {},
      page: {
        current: 1,
        size: 5,
        per_page: 5,
        total_pages: 1,
        total_elements: 42,
      },
    });
  }
});
