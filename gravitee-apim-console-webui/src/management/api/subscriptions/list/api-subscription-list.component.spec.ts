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
import { set } from 'lodash';
import { MatButtonHarness } from '@angular/material/button/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiSubscriptionListComponent } from './api-subscription-list.component';
import { ApiSubscriptionListHarness } from './api-subscription-list.harness';

import { ApiSubscriptionsModule } from '../api-subscriptions.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import {
  Api,
  ApiKeyMode,
  ApiPlansResponse,
  ApiSubscriptionsResponse,
  fakeApiV1,
  fakeApiV4,
  fakePlanV4,
  fakeSubscription,
  Plan,
  Subscription,
  VerifySubscription,
} from '../../../../entities/management-api-v2';
import { fakePagedResult, PagedResult } from '../../../../entities/pagedResult';
import { ApiKeyMode as ApplicationApiKeyMode, Application } from '../../../../entities/application/Application';
import { fakeApplication } from '../../../../entities/application/Application.fixture';
import { ApiPortalSubscriptionCreationDialogHarness } from '../components/dialogs/creation/api-portal-subscription-creation-dialog.harness';
import { PlanSecurityType } from '../../../../entities/plan';
import { SubscriptionPage } from '../../../../entities/subscription/subscription';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';

@Component({
  template: ` <api-subscription-list #apiSubscriptionList></api-subscription-list> `,
  standalone: false,
})
class TestComponent {
  @ViewChild('apiSubscriptionList') apiSubscriptionListComponent: ApiSubscriptionListComponent;
}

describe('ApiSubscriptionListComponent', () => {
  const API_ID = 'api_1';
  const PLAN_ID = 'plan_1';
  const APPLICATION_ID = 'application_1';
  const anAPI = fakeApiV4({ id: API_ID });
  const aPlan = fakePlanV4({ id: PLAN_ID, apiId: API_ID });
  const aKeylessPlan = fakePlanV4({ id: 'keyless-plan', apiId: API_ID, name: 'Keyless plan', security: { type: 'KEY_LESS' } });
  const anApplication = fakeApplication({ id: APPLICATION_ID, owner: { displayName: 'Gravitee.io' } });

  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (planSecurity?: any) => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ApiSubscriptionsModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
            isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
          },
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(
              constants,
              'env.settings.plan.security',
              planSecurity ?? {
                customApiKey: { enabled: true },
              },
            );
            return constants;
          },
        },
      ],
    }).compileComponents();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('filters tests', () => {
    beforeEach(async () => {
      await init();
    });

    it('should display default filters', fakeAsync(async () => {
      await initComponent([]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const selectedApplications = await harness.getApplications();
      expect(await selectedApplications?.length).toEqual(0);

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Accepted, Paused, Pending');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('');
    }));

    it('should init filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], [anApplication], {
        plan: PLAN_ID,
        application: APPLICATION_ID,
        status: 'CLOSED,REJECTED',
        apiKey: 'apiKey_1',
      });
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.getValueText()).toEqual('Default plan');

      const selectedApplications = await harness.getApplications();
      expect(await selectedApplications?.length).toEqual(1);
      expect(selectedApplications[0]).toEqual('Default application (Gravitee.io)');

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Closed, Rejected');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('apiKey_1');
    }));

    it('should not include keyless plan in plan filters', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan, aKeylessPlan], [anApplication], {
        plan: PLAN_ID,
        application: APPLICATION_ID,
        status: 'CLOSED,REJECTED',
        apiKey: 'apiKey_1',
      });
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const planSelectInput = await harness.getPlanSelectInput();
      await planSelectInput.open();
      const planSelectOptions = await planSelectInput.getOptions();
      expect(planSelectOptions.length).toEqual(1);
      expect(await planSelectOptions[0].getText()).toEqual('Default plan');
    }));

    it('should reset filters from params', fakeAsync(async () => {
      await initComponent([], anAPI, [aPlan], [], {
        plan: null,
        application: null,
        status: 'CLOSED,REJECTED',
        apiKey: 'apiKey_1',
      });
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const resetFilterButton = await harness.getResetFilterButton();
      await resetFilterButton.click();
      tick(800);
      expectApiSubscriptionsGetRequest([]);

      const planSelectInput = await harness.getPlanSelectInput();
      expect(await planSelectInput.isDisabled()).toEqual(false);
      expect(await planSelectInput.isEmpty()).toEqual(true);

      const selectedApplications = await harness.getApplications();
      expect(await selectedApplications?.length).toEqual(0);

      const statusSelectInput = await harness.getStatusSelectInput();
      expect(await statusSelectInput.isDisabled()).toEqual(false);
      expect(await statusSelectInput.getValueText()).toEqual('Accepted, Paused, Pending');

      const apiKeyInput = await harness.getApiKeyInput();
      expect(await apiKeyInput.isDisabled()).toEqual(false);
      expect(await apiKeyInput.getValue()).toEqual('');
    }));
  });

  describe('subscriptionsTable tests', () => {
    beforeEach(async () => {
      await init();
    });
    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          securityType: 'Security type',
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

    it('should display a table with one row and show view details button when user can create', fakeAsync(async () => {
      const subscription = fakeSubscription();
      await initComponent([subscription]);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          securityType: 'Security type',
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
          'API_KEY',
          'My Plan',
          'My Application (Primary Owner)',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '',
          'Accepted',
          '',
        ],
      ]);
      expect(
        await loader
          .getHarness(MatButtonHarness.with({ selector: '[aria-label="View the subscription details"]' }))
          .then((btn) => btn.isDisabled()),
      ).toEqual(false);
    }));

    it('should display a table with one row and show edit button when user can update', fakeAsync(async () => {
      const subscription = fakeSubscription();
      await initComponent([subscription], undefined, undefined, undefined, undefined, [
        'api-subscription-u',
        'api-subscription-r',
        'api-subscription-c',
      ]);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          securityType: 'Security type',
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
          'API_KEY',
          'My Plan',
          'My Application (Primary Owner)',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '',
          'Accepted',
          '',
        ],
      ]);
      expect(
        await loader
          .getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the subscription"]' }))
          .then((btn) => btn.isDisabled()),
      ).toEqual(false);
    }));

    it('should display a table with one row and view details button when read only', fakeAsync(async () => {
      const subscription = fakeSubscription();
      await initComponent([subscription], undefined, undefined, undefined, undefined, ['api-subscription-r']);

      const { headerCells, rowCells } = await computeSubscriptionsTableCells();
      expect(headerCells).toEqual([
        {
          securityType: 'Security type',
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
          'API_KEY',
          'My Plan',
          'My Application (Primary Owner)',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '',
          'Accepted',
          '',
        ],
      ]);
      expect(
        await loader
          .getHarness(MatButtonHarness.with({ selector: '[aria-label="View the subscription details"]' }))
          .then((btn) => btn.isDisabled()),
      ).toEqual(false);
    }));

    it('should search closed subscription', fakeAsync(async () => {
      await initComponent([fakeSubscription()]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const statusSelectInput = await getEmptyFilterForm(harness);
      await statusSelectInput.clickOptions({ text: 'Closed' });
      tick(400); // Wait for the debounce
      const closedSubscription = fakeSubscription({ status: 'CLOSED' });
      expectApiSubscriptionsGetRequest([closedSubscription], ['CLOSED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([
        [
          'API_KEY',
          'My Plan',
          'My Application (Primary Owner)',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '2020-01-01 00:00:00',
          '',
          'Closed',
          '',
        ],
      ]);
    }));

    it('should search and not find any subscription', fakeAsync(async () => {
      await initComponent([fakeSubscription()]);

      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const statusSelectInput = await getEmptyFilterForm(harness);
      await statusSelectInput.clickOptions({ text: 'Rejected' });
      tick(400); // Wait for the debounce
      expectApiSubscriptionsGetRequest([], ['REJECTED']);

      const { rowCells } = await computeSubscriptionsTableCells();
      expect(rowCells).toEqual([['There is no subscription (yet).']]);
    }));
  });

  describe('create subscription', () => {
    it('should create subscription to an API Key plan in exclusive API Key mode without customApiKey', fakeAsync(async () => {
      await init({
        customApiKey: { enabled: true },
        sharedApiKey: { enabled: true },
      });
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication();

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication('application');
      tick(400);
      const apiKeySubscription: Partial<SubscriptionPage> = {
        security: PlanSecurityType.API_KEY,
        api: 'another-plan-id',
        origin: 'MANAGEMENT',
      };
      expectSubscriptionsForApplication(application.id, [apiKeySubscription]);
      await creationDialogHarness.choosePlan(planV4.name);

      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeFalsy();

      expectApiKeySubscriptionsGetRequest(application.id, [apiKeySubscription]);
      expect(await creationDialogHarness.isApiKeyModeRadioGroupDisplayed()).toBeTruthy();
      await creationDialogHarness.chooseApiKeyMode('API Key');
      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeTruthy();

      await creationDialogHarness.createSubscription();
      tick(400);
      const subscription = fakeSubscription({ application: { apiKeyMode: 'EXCLUSIVE' } });
      expectApiSubscriptionsPostRequest(planV4.id, application.id, undefined, 'EXCLUSIVE', subscription);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'aee23b1e-34b1-4551-a23b-1e34b165516a'], expect.anything());

      flush();
    }));

    it('should create subscription to a api key plan in shared API Key mode without customApiKey', fakeAsync(async () => {
      await init({
        customApiKey: { enabled: true },
        sharedApiKey: { enabled: true },
      });
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication();

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication('application');
      tick(400);

      const apiKeySubscription = {
        security: PlanSecurityType.API_KEY,
        api: 'another-plan-id',
      };
      expectSubscriptionsForApplication(application.id, [apiKeySubscription]);

      await creationDialogHarness.choosePlan(planV4.name);

      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeFalsy();

      expectApiKeySubscriptionsGetRequest(application.id, [apiKeySubscription]);
      expect(await creationDialogHarness.isApiKeyModeRadioGroupDisplayed()).toBeTruthy();
      await creationDialogHarness.chooseApiKeyMode('Shared API Key');
      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeFalsy();

      await creationDialogHarness.createSubscription();
      tick(400);
      const subscription = fakeSubscription({ application: { apiKeyMode: 'SHARED' } });
      expectApiSubscriptionsPostRequest(planV4.id, application.id, undefined, 'SHARED', subscription);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'aee23b1e-34b1-4551-a23b-1e34b165516a'], expect.anything());

      flush();
    }));

    it('should not display custom api key for applications with share api key mode', fakeAsync(async () => {
      await init({
        customApiKey: { enabled: true },
        sharedApiKey: { enabled: true },
      });
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication({ api_key_mode: ApplicationApiKeyMode.SHARED });

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication('application');
      tick(400);
      const apiKeySubscription = {
        security: PlanSecurityType.API_KEY,
        api: 'another-plan-id',
      };
      expectSubscriptionsForApplication(application.id, [apiKeySubscription]);
      await creationDialogHarness.choosePlan(planV4.name);

      expectApiKeySubscriptionsGetRequest(application.id, [apiKeySubscription]);
      expect(await creationDialogHarness.isApiKeyModeRadioGroupDisplayed()).toBeFalsy();
      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeFalsy();
      flush();
    }));

    it('should create subscription to a API Key plan without customApiKey', fakeAsync(async () => {
      await init();
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication();

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication('application');
      tick(400);
      expectSubscriptionsForApplication(application.id, []);
      await creationDialogHarness.choosePlan(planV4.name);

      expectApiKeySubscriptionsGetRequest(application.id, []);
      await creationDialogHarness.createSubscription();
      tick(400);
      const subscription = fakeSubscription();
      expectApiSubscriptionsPostRequest(planV4.id, application.id, undefined, undefined, subscription);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'aee23b1e-34b1-4551-a23b-1e34b165516a'], expect.anything());

      flush();
    }));

    it('should create subscription to an API Key plan with customApiKey', fakeAsync(async () => {
      await init();
      const planV4 = fakePlanV4({ apiId: API_ID, generalConditions: undefined });
      const application = fakeApplication({ id: 'my-app' });

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication(application.name);
      tick(400);
      expectSubscriptionsForApplication(application.id, []);
      await creationDialogHarness.choosePlan(planV4.name);

      expectApiKeySubscriptionsGetRequest(application.id, []);
      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeTruthy();
      await creationDialogHarness.addCustomKey('12345678');
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_verify`,
        method: 'POST',
      });
      const verifySubscription: VerifySubscription = {
        applicationId: 'my-app',
        apiKey: '12345678',
      };
      expect(req.request.body).toEqual(verifySubscription);
      req.flush({ ok: true });

      await creationDialogHarness.createSubscription();
      tick(400);
      const subscription = fakeSubscription();
      expectApiSubscriptionsPostRequest(planV4.id, application.id, '12345678', undefined, subscription);

      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'aee23b1e-34b1-4551-a23b-1e34b165516a'], expect.anything());

      flush();
    }));

    it('should not create subscription on cancel', fakeAsync(async () => {
      await init();
      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }));

      const harness = await loader.getHarness(ApiSubscriptionListHarness);
      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.cancelSubscription();

      expect(routerNavigateSpy).not.toHaveBeenCalledWith(['.', 'aee23b1e-34b1-4551-a23b-1e34b165516a'], expect.anything());

      flush();
    }));

    it('API created with the kubernetes operator should not be able to create subscription', fakeAsync(async () => {
      await init();
      await initComponent([], fakeApiV4({ id: API_ID, definitionContext: { origin: 'KUBERNETES' } }));

      const harness = await loader.getHarness(ApiSubscriptionListHarness);
      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);
    }));

    it('V1 API should not be able to create subscription', fakeAsync(async () => {
      await init();
      await initComponent([], fakeApiV1({ id: API_ID }));

      const harness = await loader.getHarness(ApiSubscriptionListHarness);
      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(true);
    }));

    it('should not display custom api key nor API Key mode for federated API', fakeAsync(async () => {
      await init({
        customApiKey: { enabled: true },
        sharedApiKey: { enabled: true },
      });
      const planV4 = fakePlanV4({ generalConditions: undefined });
      const application = fakeApplication({ api_key_mode: ApplicationApiKeyMode.SHARED });

      await initComponent([], fakeApiV4({ id: API_ID, listeners: [] }), [planV4]);
      const harness = await loader.getHarness(ApiSubscriptionListHarness);

      const createSubBtn = await harness.getCreateSubscriptionButton();
      expect(await createSubBtn).toBeDefined();
      expect(await createSubBtn.isDisabled()).toEqual(false);

      await createSubBtn.click();

      const creationDialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(
        ApiPortalSubscriptionCreationDialogHarness,
      );
      expect(await creationDialogHarness.getTitleText()).toEqual('Create a subscription');

      await creationDialogHarness.searchApplication('application');
      tick(400);
      expectApplicationsSearch('application', [application]);
      tick();
      await creationDialogHarness.selectApplication('application');
      tick(400);
      const apiKeySubscription = {
        security: PlanSecurityType.API_KEY,
        api: 'another-plan-id',
      };
      expectSubscriptionsForApplication(application.id, [apiKeySubscription]);
      await creationDialogHarness.choosePlan(planV4.name);

      expectApiKeySubscriptionsGetRequest(application.id, [apiKeySubscription]);
      expect(await creationDialogHarness.isApiKeyModeRadioGroupDisplayed()).toBeFalsy();
      expect(await creationDialogHarness.isCustomApiKeyInputDisplayed()).toBeFalsy();
      flush();
    }));
  });

  describe('export subscriptions', () => {
    beforeEach(async () => {
      await init();
    });
    it('should not export subscriptions if no subscription', fakeAsync(async () => {
      await initComponent([]);

      const harness = await loader.getHarness(ApiSubscriptionListHarness);
      const exportBtn = await harness.getExportButton();
      expect(await exportBtn.isDisabled()).toEqual(true);
    }));

    it('should export subscriptions with same filters except pagination', fakeAsync(async () => {
      const listedSubscriptions = [
        fakeSubscription(),
        fakeSubscription(),
        fakeSubscription(),
        fakeSubscription(),
        fakeSubscription(),
        fakeSubscription(),
      ];
      await initComponent(listedSubscriptions, anAPI, [aPlan], [anApplication], {
        plan: aPlan.id,
        application: anApplication.id,
        status: 'ACCEPTED,CLOSED,RESUMED',
        apiKey: '12345678',
      });

      const harness = await loader.getHarness(ApiSubscriptionListHarness);
      const exportBtn = await harness.getExportButton();
      expect(await exportBtn.isDisabled()).toEqual(false);

      await exportBtn.click();
      tick(400);
      expectExportGetRequest(listedSubscriptions, ['ACCEPTED', 'CLOSED', 'RESUMED'], [anApplication.id], [aPlan.id], '12345678');
      flush();
    }));
  });

  async function initComponent(
    subscriptions: Subscription[],
    api: Api = anAPI,
    plans: Plan[] = [aPlan],
    applications?: Application[],
    params?: { plan?: string; application?: string; status?: string; apiKey?: string },
    permissions: string[] = ['api-subscription-c', 'api-subscription-r'],
  ) {
    await TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: {
          params: { apiId: api.id },
          queryParams: { ...(params ? params : {}) },
        },
      },
    }).compileComponents();
    if (permissions.length > 0) {
      await TestBed.overrideProvider(GioTestingPermissionProvider, { useValue: permissions }).compileComponents();
    }
    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();

    tick(800); // wait for debounce
    expectApiGetRequest(api);
    expectApiPlansGetRequest(plans);

    if (params?.application) {
      params?.application.split(',').forEach((appId) =>
        expectGetApplication(
          appId,
          applications.find((app) => app.id === appId),
        ),
      );
    }
    expectApiSubscriptionsGetRequest(
      subscriptions,
      params?.status?.split(','),
      params?.application?.split(','),
      params?.plan?.split(','),
      params?.apiKey,
    );

    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    tick(400);

    expect(fixture.componentInstance.apiSubscriptionListComponent.isLoadingData).toEqual(false);
    expect(fixture.componentInstance.apiSubscriptionListComponent.canUpdate).toEqual(permissions.includes('api-subscription-u'));
    expect(fixture.componentInstance.apiSubscriptionListComponent.subscriptionsTableDS).toBeDefined();
    expect(fixture.componentInstance.apiSubscriptionListComponent.subscriptionsTableDS.length).toEqual(subscriptions.length);
  }

  async function computeSubscriptionsTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#subscriptionsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  async function getEmptyFilterForm(harness: ApiSubscriptionListHarness) {
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

  function expectApiSubscriptionsGetRequest(
    subscriptions: Subscription[] = [],
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
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
          apiKey ? `&apiKey=${apiKey}` : ''
        }&expands=application,plan`,
        method: 'GET',
      })
      .flush(response);
    fixture.detectChanges();
  }

  function expectApiSubscriptionsPostRequest(
    planId: string,
    applicationId: string,
    customApiKey: string = undefined,
    apiKeyMode: ApiKeyMode = undefined,
    subscription: Subscription,
  ) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({
      planId,
      applicationId,
      customApiKey,
      apiKeyMode,
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

  function expectGetApplication(applicationId: string, application: Application) {
    const testRequest = httpTestingController
      .match(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}`)
      .find((request) => !request.cancelled);
    if (testRequest) {
      testRequest.flush(application);
      fixture.detectChanges();
    }
  }

  function expectSubscriptionsForApplication(applicationId: string, subscriptions: Partial<SubscriptionPage>[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/subscriptions?expand=security`,
        method: 'GET',
      })
      .flush(<PagedResult>{
        data: [...subscriptions],
      });
    fixture.detectChanges();
  }

  function expectExportGetRequest(
    subscriptions: Subscription[] = [],
    statuses?: string[],
    applicationIds?: string[],
    planIds?: string[],
    apiKey?: string,
  ) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/subscriptions/_export?page=1&perPage=${subscriptions.length}${
          statuses ? `&statuses=${statuses.join(',')}` : '&statuses=ACCEPTED,PAUSED,PENDING'
        }${applicationIds ? `&applicationIds=${applicationIds.join(',')}` : ''}${planIds ? `&planIds=${planIds.join(',')}` : ''}${
          apiKey ? `&apiKey=${apiKey}` : ''
        }`,
        method: 'GET',
      })
      .flush(new Blob(['a'], { type: 'text/csv' }));
    fixture.detectChanges();
  }

  function expectApiKeySubscriptionsGetRequest(applicationId: string, subscriptions: Partial<SubscriptionPage>[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/subscriptions?page=1&size=20&status=ACCEPTED,PENDING,PAUSED&security_types=API_KEY`,
        method: 'GET',
      })
      .flush(fakePagedResult(subscriptions));
    fixture.detectChanges();
  }
});
