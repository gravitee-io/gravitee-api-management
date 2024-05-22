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
import { MatTableHarness } from '@angular/material/table/testing';

import { ApiTabSubscriptionsComponent } from './api-tab-subscriptions.component';
import { Subscription } from '../../../entities/subscription/subscription';
import { fakeSubscriptionResponse } from '../../../entities/subscription/subscription.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('ApiTabSubscriptionsComponent', () => {
  let fixture: ComponentFixture<ApiTabSubscriptionsComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiTabSubscriptionsComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiTabSubscriptionsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.componentInstance.apiId = 'testId';
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('empty component', () => {
    it('should show empty Subscription list', async () => {
      expectSubscriptionList(fakeSubscriptionResponse({ data: [] }), 'testId');
      expect(fixture.nativeElement.querySelector('#no-subscriptions')).toBeDefined();
    });
  });

  describe('populated subscription list', () => {
    beforeEach(() => {
      expectSubscriptionList(fakeSubscriptionResponse(), 'testId');
    });

    it('should show subscription list', async () => {
      const subscriptionTable = await harnessLoader.getHarness(MatTableHarness.with({ selector: '.api-tab-subscriptions__table' }));
      expect(subscriptionTable).toBeTruthy();
      expect(await subscriptionTable.getRows().then(value => value[0].getCellTextByColumnName())).toEqual({
        application: 'Testapplication',
        expand: 'arrow_right',
        plan: 'Testplan',
        status: 'Rejected',
      });
    });
  });

  function expectSubscriptionList(subscriptionResponse: Subscription = fakeSubscriptionResponse(), apiId: string) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=${apiId}`).flush(subscriptionResponse);
  }
});
