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

import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UIRouterGlobals } from '@uirouter/core';

import { ApiPortalSubscriptionListComponent } from './api-portal-subscription-list.component';
import { ApiPortalSubscriptionListHarness } from './api-portal-subscription-list.harness';

import { ApiPortalSubscriptionsModule } from '../api-portal-subscriptions.module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { User as DeprecatedUser } from '../../../../../entities/user';
import {
  Api,
  ApiPlansResponse,
  ApiSubscribersResponse,
  ApiSubscriptionsResponse,
  BaseApplication,
  fakeApiV4,
  fakeBaseApplication,
  fakePlanV4,
  fakeSubscription,
  Plan,
  Subscription,
} from '../../../../../entities/management-api-v2';
import { PagedResult } from '../../../../../entities/pagedResult';
import { Application } from '../../../../../entities/application/application';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { ApiPortalSubscriptionCreationDialogHarness } from '../components/creation-dialog/api-portal-subscription-creation-dialog.harness';

@Component({
  template: ` <api-portal-subscription-list #apiPortalSubscriptionList></api-portal-subscription-list> `,
})
class TestComponent {
  @ViewChild('apiPortalSubscriptionList') apiPortalSubscriptionList: ApiPortalSubscriptionListComponent;
}

describe('ApiPortalSubscriptionListComponent', () => {
  const API_ID = 'api_1';
  const PLAN_ID = 'plan_1';
  const APPLICATION_ID = 'application_1';
  const anAPI = fakeApiV4({ id: API_ID });
  const aPlan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });
  const aBaseApplication = fakeBaseApplication({ id: APPLICATION_ID });
  const fakeUiRouter = { go: jest.fn() };
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['api-subscription-c', 'api-subscription-r'];

  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (ajsGlobals: any = {}) => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
            isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
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
      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const applicationSelectInput = await harness.getApplicationSelectInput();
      expect(await applicationSelectInput.isDisabled()).toEqual(false);
      expect(await applicationSelectInput.isEmpty()).toEqual(true);

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Accepted, Paused, Pending');

      const apikeyInput = await harness.getApiKeyInput();
      expect(await apikeyInput.isDisabled()).toEqual(false);
      expect(await apikeyInput.getValue()).toEqual('');
    }));

    it('should init filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], [aBaseApplication], {
        plan: PLAN_ID,
        application: APPLICATION_ID,
        status: 'CLOSED,REJECTED',
        apikey: 'apikey_1',
      });
      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.getValueText()).toEqual('Default plan');

      const applicationSelectInput = await harness.getApplicationSelectInput();
      expect(await applicationSelectInput.isDisabled()).toEqual(false);
      expect(await applicationSelectInput.getValueText()).toEqual('My first application');

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Closed, Rejected');

      const apikeyInput = await harness.getApiKeyInput();
      expect(await apikeyInput.isDisabled()).toEqual(false);
      expect(await apikeyInput.getValue()).toEqual('apikey_1');
    }));

    it('should reset filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], [aBaseApplication], {
        plan: null,
        application: null,
        status: 'CLOSED,REJECTED',
        apikey: 'apikey_1',
      });
      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const resetFilterButton = await harness.getResetFilterButton();
      await resetFilterButton.click();
      tick(800);
      expectApiSubscriptionsGetRequest([]);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const applicationSelectInput = await harness.getApplicationSelectInput();
      expect(await applicationSelectInput.isDisabled()).toEqual(false);
      expect(await applicationSelectInput.isEmpty()).toEqual(true);

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Accepted, Paused, Pending');

      const apikeyInput = await harness.getApiKeyInput();
      expect(await apikeyInput.isDisabled()).toEqual(false);
      expect(await apikeyInput.getValue()).toEqual('');
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
      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const statusSelectInput = await getEmptyFilterForm(harness);
      await statusSelectInput.clickOptions({ text: 'Closed' });
      tick(400); // Wait for the debounce
      const closedSubscription = fakeSubscription({ status: 'CLOSED' });
      expectApiSubscriptionsGetRequest([closedSubscription], ['CLOSED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([
        ['My Plan', 'My Application', 'Jan 1, 2020, 12:00:00 AM', 'Jan 1, 2020, 12:00:00 AM', 'Jan 1, 2020, 12:00:00 AM', '', 'Closed', ''],
      ]);
    }));

    it('should search and not find any subscription', fakeAsync(async () => {
      await initComponent([fakeSubscription()]);

      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const statusSelectInput = await getEmptyFilterForm(harness);
      await statusSelectInput.clickOptions({ text: 'Rejected' });
      tick(400); // Wait for the debounce
      expectApiSubscriptionsGetRequest([], ['REJECTED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([['There is no subscription (yet).']]);
    }));
  });

  describe('create subscription', () => {
    it('should create subscription to a api key plan', fakeAsync(async () => {
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication();

      await initComponent([], anAPI, [planV4]);
      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await harness.openCreationDialog();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.choosePlan(planV4.name);
      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);

      await creationDialogHarness.selectApplication(application.name);

      await creationDialogHarness.createSubscription();
      tick(400);
      const subscription = fakeSubscription();
      expectApiSubscriptionsPostRequest(planV4.id, application.id, subscription);

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.ng.subscription.edit', { subscriptionId: expect.any(String) });

      flush();
    }));

    it('should not create subscription on cancel', fakeAsync(async () => {
      await initComponent([]);

      const harness = await loader.getHarness(ApiPortalSubscriptionListHarness);
      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await harness.openCreationDialog();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.cancelSubscription();

      expect(fakeUiRouter.go).not.toHaveBeenCalledWith('management.apis.ng.subscription.edit', { subscriptionId: expect.any(String) });

      flush();
    }));
  });

  async function initComponent(
    subscriptions: Subscription[],
    api: Api = anAPI,
    plans: Plan[] = [aPlan],
    applications: BaseApplication[] = [aBaseApplication],
    params?: { plan?: string; application?: string; status?: string; apikey?: string },
  ) {
    await TestBed.overrideProvider(UIRouterStateParams, { useValue: { apiId: api.id, ...(params ? params : {}) } }).compileComponents();
    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    tick(800); // wait for debounce
    expectApiGetRequest(api);
    expectApiPlansGetRequest(plans);
    expectApiSubscribersGetRequest(applications);
    expectApiSubscriptionsGetRequest(
      subscriptions,
      params?.status?.split(','),
      params?.application?.split(','),
      params?.plan?.split(','),
      params?.apikey,
    );
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    tick(400);

    expect(fixture.componentInstance.apiPortalSubscriptionList.isLoadingData).toEqual(false);
    expect(fixture.componentInstance.apiPortalSubscriptionList.isReadOnly).toEqual(false);
    expect(fixture.componentInstance.apiPortalSubscriptionList.subscriptionsTableDS).toBeDefined();
    expect(fixture.componentInstance.apiPortalSubscriptionList.subscriptionsTableDS.length).toEqual(subscriptions.length);
  }

  async function computeSubscriptionsTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#subscriptionsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  async function getEmptyFilterForm(harness: ApiPortalSubscriptionListHarness) {
    const statusSelectInput = await harness.getStatusSelectInput();
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

  function expectApiSubscribersGetRequest(applications: BaseApplication[]) {
    const response: ApiSubscribersResponse = {
      data: applications,
      pagination: {
        totalCount: applications.length,
      },
    };
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscribers?page=1&perPage=9999`,
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

  function expectApiSubscriptionsPostRequest(planId: string, applicationId: string, subscription: Subscription) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({
      planId,
      applicationId,
      customApiKey: undefined,
    });
    req.flush(subscription);
    fixture.detectChanges();
  }

  function expectApplicationsSearch(searchTerm: string, applications: Application[]) {
    const response: PagedResult<Application> = new PagedResult<Application>();
    response.populate({
      data: applications,
      page: {
        per_page: 20,
        total_elements: applications.length,
        current: 1,
        size: applications.length,
        total_pages: 1,
      },
      metadata: {},
    });
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=1&size=20&status=ACTIVE&query=${searchTerm}&order=name`,
        method: 'GET',
      })
      .flush(response);
    fixture.detectChanges();
  }
});
