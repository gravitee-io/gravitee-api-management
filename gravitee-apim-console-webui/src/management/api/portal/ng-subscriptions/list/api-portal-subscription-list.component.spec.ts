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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { UIRouterGlobals } from '@uirouter/core';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiPortalSubscriptionListComponent } from './api-portal-subscription-list.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiPortalSubscriptionsModule } from '../api-portal-subscriptions.module';
import { User as DeprecatedUser } from '../../../../../entities/user';
import {
  Api,
  ApiPlansResponse,
  ApiSubscriptionsResponse,
  fakeApiV4,
  fakePlanV4,
  fakeSubscription,
  Plan,
  Subscription,
} from '../../../../../entities/management-api-v2';

describe('ApiPortalSubscriptionListComponent', () => {
  const API_ID = 'api#1';
  const PLAN_ID = 'plan#1';
  const anAPI = fakeApiV4({ id: API_ID });
  const aPlan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['api-plan-u', 'api-plan-r', 'api-plan-d'];

  let fixture: ComponentFixture<ApiPortalSubscriptionListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (ajsGlobals: any = {}) => {
    await TestBed.configureTestingModule({
      imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        },
        {
          provide: UIRouterGlobals,
          useValue: ajsGlobals,
        },
      ],
    }).compileComponents();
  };

  beforeEach(async () => {
    await init();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('filters tests', () => {
    it('should display default filters', fakeAsync(async () => {
      await initComponent([]);

      const planSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="planIds"]' }));
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const applicationSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="applicationIds"]' }));
      expect(await applicationSelectInput.isDisabled()).toEqual(false);
      expect(await applicationSelectInput.isEmpty()).toEqual(true);

      const statusSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="statuses"]' }));
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Accepted, Paused, Pending');

      const apikeyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="apikey"]' }));
      expect(await apikeyInput.isDisabled()).toEqual(false);
      expect(await apikeyInput.getValue()).toEqual('');
    }));

    it('should init filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], { plan: null, application: null, status: 'CLOSED,REJECTED', apikey: 'apikey_1' });

      const planSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="planIds"]' }));
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const applicationSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="applicationIds"]' }));
      expect(await applicationSelectInput.isDisabled()).toEqual(false);
      expect(await applicationSelectInput.isEmpty()).toEqual(true);

      const statusSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="statuses"]' }));
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Closed, Rejected');

      const apikeyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="apikey"]' }));
      expect(await apikeyInput.isDisabled()).toEqual(false);
      expect(await apikeyInput.getValue()).toEqual('apikey_1');
    }));

    it('should reset filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], { plan: null, application: null, status: 'CLOSED,REJECTED', apikey: 'apikey_1' });

      const resetFilterButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Reset filters"]' }));
      await resetFilterButton.click();
      tick(800);
      expectApiSubscriptionsGetRequest([]);
    }));
  });

  describe('subscriptionsTable tests', () => {
    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          plan: 'Plan',
          application: 'Application',
          createdAt: 'Created at',
          processedAt: 'Processed at',
          startingAt: 'Start at',
          endAt: 'End at',
          status: 'Status',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([['There is no subscription (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const subscription = fakeSubscription();
      await initComponent([subscription]);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          plan: 'Plan',
          application: 'Application',
          createdAt: 'Created at',
          processedAt: 'Processed at',
          startingAt: 'Start at',
          endAt: 'End at',
          status: 'Status',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([
        [
          'My Plan',
          'My Application',
          'Jan 1, 2020, 12:00:00 AM',
          'Jan 1, 2020, 12:00:00 AM',
          'Jan 1, 2020, 12:00:00 AM',
          '',
          'Accepted',
          '',
        ],
      ]);
    }));

    it('should search closed subscription', fakeAsync(async () => {
      await initComponent([fakeSubscription()]);

      const statusSelectInput = await getEmptyFilterForm();
      await statusSelectInput.clickOptions({ text: 'Closed' });
      tick(800); // Wait for the debounce
      const closedSubscription = fakeSubscription({ status: 'CLOSED' });
      expectApiSubscriptionsGetRequest([closedSubscription], ['CLOSED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([
        ['My Plan', 'My Application', 'Jan 1, 2020, 12:00:00 AM', 'Jan 1, 2020, 12:00:00 AM', 'Jan 1, 2020, 12:00:00 AM', '', 'Closed', ''],
      ]);
    }));

    it('should search and not find any subscription', fakeAsync(async () => {
      await initComponent([fakeSubscription()]);

      const statusSelectInput = await getEmptyFilterForm();
      await statusSelectInput.clickOptions({ text: 'Rejected' });
      tick(800); // Wait for the debounce
      expectApiSubscriptionsGetRequest([], ['REJECTED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([['There is no subscription (yet).']]);
    }));
  });

  async function initComponent(
    subscriptions: Subscription[],
    api: Api = anAPI,
    plans: Plan[] = [aPlan],
    params?: { plan?: string; application?: string; status?: string; apikey?: string },
  ) {
    await TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: api.id, ...(params ? params : {}) } }).compileComponents();
    fixture = TestBed.createComponent(ApiPortalSubscriptionListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    tick(800); // wait for debounce
    expectApiGetRequest(api);
    expectApiPlansGetRequest(plans);
    expectApiSubscriptionsGetRequest(
      subscriptions,
      params?.status?.split(','),
      params?.application?.split(','),
      params?.plan?.split(','),
      params?.apikey,
    );

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  async function computeSubscriptionsTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#subscriptionsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  async function getEmptyFilterForm() {
    const statusSelectInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="statuses"]' }));
    expect(await statusSelectInput.isDisabled()).toEqual(false);

    // remove default selected values
    await statusSelectInput.clickOptions({ text: 'Accepted' });
    await statusSelectInput.clickOptions({ text: 'Paused' });
    await statusSelectInput.clickOptions({ text: 'Pending' });

    return statusSelectInput;
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
        method: 'GET',
      })
      .flush(api);
    fixture.detectChanges();
  }

  function expectApiPlansGetRequest(plans: Plan[]) {
    const response: ApiPlansResponse = {
      data: plans,
      pagination: {
        totalCount: plans.length,
      },
    };
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999`,
        method: 'GET',
      })
      .flush(response);
    fixture.detectChanges();
  }

  function expectApiSubscriptionsGetRequest(
    subscriptions: Subscription[] = [],
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apikey?: string,
  ) {
    const response: ApiSubscriptionsResponse = {
      data: subscriptions,
      pagination: {
        totalCount: subscriptions.length,
      },
      links: {},
    };
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions?page=1&perPage=10${
          statuses ? `&statuses=${statuses.join(',')}` : '&statuses=ACCEPTED,PAUSED,PENDING'
        }${applicationIds ? `&applicationIds=${applicationIds.join(',')}` : ''}${planIds ? `&planIds=${planIds.join(',')}` : ''}${
          apikey ? `&apikey=${apikey}` : ''
        }&expands=application,plan`,
        method: 'GET',
      })
      .flush(response);
    fixture.detectChanges();
  }
});
