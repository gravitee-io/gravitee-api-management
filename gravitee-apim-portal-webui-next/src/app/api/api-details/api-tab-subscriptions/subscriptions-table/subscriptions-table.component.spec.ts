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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { SubscriptionsTableComponent } from './subscriptions-table.component';
import { fakeSubscriptionResponse } from '../../../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../../../entities/subscription/subscriptions-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

describe('SubscriptionsTableComponent', () => {
  let fixture: ComponentFixture<SubscriptionsTableComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsTableComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsTableComponent);
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
      expectSubscriptionList(fakeSubscriptionResponse({ data: [] }), 'testId', '');
      expect(fixture.nativeElement.querySelector('#no-subscriptions')).toBeDefined();
      expect(await getStatusSelectionOptional()).toEqual(null);
    });
  });

  describe('populated subscription list', () => {
    beforeEach(() => {
      expectSubscriptionList(fakeSubscriptionResponse(), 'testId', '');
    });

    it('should show subscription list', async () => {
      const subscriptionTable = await harnessLoader.getHarness(MatTableHarness.with({ selector: '.api-tab-subscriptions__table' }));
      expect(subscriptionTable).toBeTruthy();
      expect(await subscriptionTable.getRows().then(value => value[0].getCellTextByColumnName())).toEqual({
        application: 'testApplication',
        expand: 'arrow_right',
        plan: '-',
        status: 'Rejected',
      });
      expect(await getStatusSelectionOptional()).toBeTruthy();
    });

    it('should show filtered subscription list', async () => {
      const filterSelect = await harnessLoader.getHarness(MatSelectHarness.with({ selector: '[id=api-tab-subscription__select]' }));
      expect(filterSelect).toBeTruthy();
      await filterSelect.clickOptions({ text: 'Pending' });
      expect(await filterSelect.getValueText()).toBe('Pending');

      expectSubscriptionList(fakeSubscriptionResponse(), 'testId', 'PENDING');
    });
  });

  function expectSubscriptionList(subscriptionResponse: SubscriptionsResponse = fakeSubscriptionResponse(), apiId: string, status: string) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=${apiId}${status ? '&statuses=' + `${status}` : ''}&size=-1`)
      .flush(subscriptionResponse);
  }

  async function getStatusSelectionOptional(): Promise<MatSelectHarness | null> {
    return await harnessLoader.getHarnessOrNull(MatSelectHarness);
  }
});
