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

import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApplicationSubscriptionCreationDialogHarness } from './application-subscription-creation-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { fakePagedResult } from '../../../../../entities/pagedResult';
import { EnvironmentSettingsService } from '../../../../../services-ngx/environment-settings.service';
import { EnvSettings } from '../../../../../entities/Constants';
import { ApplicationSubscriptionListHarness } from '../list/application-subscription-list.harness';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApplicationSubscriptionListComponent } from '../list/application-subscription-list.component';
import { ApplicationSubscriptionListModule } from '../list/application-subscription-list.module';
import {
  Api,
  ConnectorPlugin,
  fakeApiFederated,
  fakeApiV2,
  fakeApiV4,
  fakePlanFederated,
  fakePlanV2,
  fakePlanV4,
  getEntrypointConnectorSchema,
  Plan,
} from '../../../../../entities/management-api-v2';
import { ApiKeyMode, Application } from '../../../../../entities/application/Application';
import { fakeSubscriptionPage } from '../../../../../entities/subscription/subscription.fixture';
import { PlanSecurityType } from '../../../../../entities/plan';
import { SubscriptionPage } from '../../../../../entities/subscription/subscription';
import { NewSubscriptionEntity } from '../../../../../entities/application';
import { fakeNewSubscriptionEntity } from '../../../../../entities/application/NewSubscriptionEntity.fixtures';

describe('ApplicationSubscriptionCreationDialogComponent', () => {
  const APPLICATION_ID = 'app-id';
  const APP = fakeApplication({ id: APPLICATION_ID, api_key_mode: ApiKeyMode.UNSPECIFIED });
  const API_ID = 'api-id';
  const ANOTHER_API_ID = 'another-api-id';
  const DEFAULT_PERMISSIONS = ['application-subscription-c', 'application-subscription-r'];
  const DEFAULT_ENV_SETTINGS = {
    plan: {
      security: {
        apikey: { enabled: true },
        jwt: { enabled: true },
        oauth2: { enabled: true },
        keyless: { enabled: true },
        customApiKey: { enabled: false },
        sharedApiKey: { enabled: false },
        mtls: { enabled: false },
      },
    },
  };
  const WEBHOOK_ENTRYPOINT: ConnectorPlugin = {
    id: 'webhook',
    name: 'Webhook',
    description: 'Webhook entrypoint',
    icon: 'webhook-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    availableFeatures: ['DLQ'],
    supportedListenerType: 'SUBSCRIPTION',
  };
  const ENTRYPOINT_LIST: ConnectorPlugin[] = [
    WEBHOOK_ENTRYPOINT,
    {
      id: 'http-get',
      name: 'HTTP Get',
      description: 'HTTP Get entrypoint',
      icon: 'http-get-icon',
      deployed: true,
      supportedApiType: 'MESSAGE',
      availableFeatures: [],
      supportedListenerType: 'HTTP',
    },
  ];
  const API_KEY_PLAN = fakePlanV2({
    apiId: ANOTHER_API_ID,
    name: 'plan',
    security: { type: 'API_KEY' },
    commentRequired: false,
    generalConditions: null,
  });
  const API_KEY_SUBSCRIPTION = fakeSubscriptionPage({
    api: ANOTHER_API_ID,
    plan: API_KEY_PLAN.id,
    security: PlanSecurityType.API_KEY,
  });

  let fixture: ComponentFixture<ApplicationSubscriptionListComponent>;
  let httpTestingController: HttpTestingController;
  let harness: ApplicationSubscriptionListHarness;
  let dialogHarness: ApplicationSubscriptionCreationDialogHarness;
  let rootLoader: HarnessLoader;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async (app: Application = APP, snapshot: Partial<EnvSettings> = DEFAULT_ENV_SETTINGS) => {
    await TestBed.configureTestingModule({
      imports: [ApplicationSubscriptionListModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: InteractivityChecker, useValue: { isFocusable: () => true, isTabbable: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { applicationId: APPLICATION_ID }, queryParams: {} } },
        },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot } },
        { provide: GioTestingPermissionProvider, useValue: DEFAULT_PERMISSIONS },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationSubscriptionListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSubscriptionListHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    routerNavigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate');
    fixture.detectChanges();

    // init  subscriptions list component
    tick(800);
    expectApplicationGetRequest(app);
    expectSubscriptionsGetRequest([API_KEY_SUBSCRIPTION]);
    expectApiGetRequest(fakeApiV2({ id: ANOTHER_API_ID }));

    // open subscription's creation dialog
    await harness.createSubscription();
    dialogHarness = await rootLoader.getHarness(ApplicationSubscriptionCreationDialogHarness);

    expectSubscriptionsGetRequest([API_KEY_SUBSCRIPTION], '20', undefined, 'API_KEY');
    expectEntrypointListGet();
  };

  afterEach(() => httpTestingController.verify());

  describe('subscription creation test', () => {
    const API_NAME = 'my-api';
    const API = fakeApiV4({ id: API_ID, name: API_NAME });

    describe('With api key plan', () => {
      const API_KEY_PLAN = fakePlanV4({ apiId: API_ID, security: { type: 'API_KEY' }, commentRequired: false, generalConditions: null });

      it('should create a subscription with a random key', fakeAsync(async () => {
        await init(APP, {
          plan: {
            security: {
              ...DEFAULT_ENV_SETTINGS.plan.security,
              sharedApiKey: { enabled: true },
            },
          },
        });

        await dialogHarness.searchApi(API_NAME);
        tick(100);
        expectApiSearchPost(API);

        await dialogHarness.selectApi(API_NAME);
        expectSubscribableApiPlansGet([API_KEY_PLAN, fakePlanV4({ apiId: API_ID, security: { type: 'JWT' } })]);

        await dialogHarness.selectPlan(API_KEY_PLAN.name);
        await dialogHarness.selectApiKeyMode('API Key');
        await dialogHarness.createSubscription();

        const subscription = fakeNewSubscriptionEntity({ apiKeyMode: ApiKeyMode.UNSPECIFIED });
        expectApiSubscriptionsPostRequest(subscription, API_KEY_PLAN.id, ApiKeyMode.EXCLUSIVE);
        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
      }));

      it('should create a subscription with shared api key', fakeAsync(async () => {
        await init(APP, {
          plan: {
            security: {
              ...DEFAULT_ENV_SETTINGS.plan.security,
              sharedApiKey: { enabled: true },
            },
          },
        });

        await dialogHarness.searchApi(API_NAME);
        tick(100);
        expectApiSearchPost(API);

        await dialogHarness.selectApi(API_NAME);
        expectSubscribableApiPlansGet([API_KEY_PLAN, fakePlanV4({ apiId: API_ID, security: { type: 'JWT' } })]);

        await dialogHarness.selectPlan(API_KEY_PLAN.name);
        await dialogHarness.selectApiKeyMode('Shared API Key');
        await dialogHarness.createSubscription();

        const subscription = fakeNewSubscriptionEntity({ apiKeyMode: ApiKeyMode.SHARED });
        expectApiSubscriptionsPostRequest(subscription, API_KEY_PLAN.id, ApiKeyMode.SHARED);
        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
      }));

      it('should create a subscription for Federated API', fakeAsync(async () => {
        await init(APP, {
          plan: {
            security: {
              ...DEFAULT_ENV_SETTINGS.plan.security,
              sharedApiKey: { enabled: true },
            },
          },
        });

        await dialogHarness.searchApi(API_NAME);
        tick(100);
        expectApiSearchPost(fakeApiFederated({ id: API_ID, name: API_NAME }));

        await dialogHarness.selectApi(API_NAME);
        expectSubscribableApiPlansGet([API_KEY_PLAN, fakePlanFederated({ apiId: API_ID, security: { type: 'JWT' } })]);

        await dialogHarness.selectPlan(API_KEY_PLAN.name);
        await dialogHarness.createSubscription();

        const subscription = fakeNewSubscriptionEntity({ apiKeyMode: ApiKeyMode.UNSPECIFIED });
        expectApiSubscriptionsPostRequest(subscription, API_KEY_PLAN.id);
        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
      }));
    });

    it.each(['JWT', 'OAUTH2'])(
      'should not be able to create subscription when application client_id is null',
      fakeAsync(async (planType: PlanSecurityType) => {
        const app = fakeApplication({ id: APPLICATION_ID, api_key_mode: ApiKeyMode.UNSPECIFIED });
        app.settings.app = { client_id: null };
        app.settings.oauth = { client_id: null };
        await init(app);

        await dialogHarness.searchApi(API_NAME);
        tick(200);
        expectApiSearchPost(API);
        await dialogHarness.selectApi(API_NAME);

        const plan = fakePlanV4({ apiId: API_ID, security: { type: planType }, commentRequired: false, generalConditions: null });
        expectSubscribableApiPlansGet([plan]);

        tick(100);
        await dialogHarness.selectPlan(plan.name);

        expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeTruthy();
      }),
    );

    it.each(['JWT', 'OAUTH2'])(
      'should create the subscription for %s plan with app client id',
      fakeAsync(async (planType: PlanSecurityType) => {
        const app = fakeApplication({ id: APPLICATION_ID, api_key_mode: ApiKeyMode.UNSPECIFIED });
        app.settings.app = { client_id: 'client-id' };
        app.settings.oauth = { client_id: null };
        await init(app);

        await dialogHarness.searchApi(API_NAME);
        tick(100);
        expectApiSearchPost(API);
        await dialogHarness.selectApi(API_NAME);

        const plan = fakePlanV4({
          name: `${planType} plan`,
          apiId: API_ID,
          security: { type: planType },
          commentRequired: false,
          generalConditions: null,
        });
        expectSubscribableApiPlansGet([fakePlanV4({ apiId: API_ID, security: { type: 'API_KEY' } }), plan]);

        tick(100);
        await dialogHarness.selectPlan(plan.name);

        expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeFalsy();
        await dialogHarness.createSubscription();

        const subscription = fakeNewSubscriptionEntity();
        expectApiSubscriptionsPostRequest(subscription, plan.id);
        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
      }),
    );

    it.each(['JWT', 'OAUTH2'])(
      'should create the subscription for %s plan with oauth client id (DCR case)',
      fakeAsync(async (planType: PlanSecurityType) => {
        const app = fakeApplication({ id: APPLICATION_ID, api_key_mode: ApiKeyMode.UNSPECIFIED });
        app.settings.app = { client_id: null };
        app.settings.oauth = { client_id: 'client-id' };
        await init(app);

        await dialogHarness.searchApi(API_NAME);
        tick(100);
        expectApiSearchPost(API);
        await dialogHarness.selectApi(API_NAME);

        const plan = fakePlanV4({
          name: `${planType} plan`,
          apiId: API_ID,
          security: { type: planType },
          commentRequired: false,
          generalConditions: null,
        });
        expectSubscribableApiPlansGet([fakePlanV4({ apiId: API_ID, security: { type: 'API_KEY' } }), plan]);

        tick(100);
        await dialogHarness.selectPlan(plan.name);

        expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeFalsy();
        await dialogHarness.createSubscription();

        const subscription = fakeNewSubscriptionEntity();
        expectApiSubscriptionsPostRequest(subscription, plan.id);
        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
      }),
    );

    it('should add request message on subscribe', fakeAsync(async () => {
      await init({ ...APP, settings: { ...APP.settings, app: { client_id: 'client-id' } } });

      await dialogHarness.searchApi(API_NAME);
      tick(100);
      expectApiSearchPost(API);
      await dialogHarness.selectApi(API_NAME);

      const plan = fakePlanV4({
        name: 'JWT plan',
        apiId: API_ID,
        security: { type: 'JWT' },
        commentRequired: true,
        generalConditions: null,
      });
      expectSubscribableApiPlansGet([fakePlanV4({ apiId: API_ID, security: { type: 'API_KEY' } }), plan]);

      tick(100);
      await dialogHarness.selectPlan(plan.name);

      expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeTruthy();
      await dialogHarness.addRequestMessage('message');
      expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeFalsy();

      await dialogHarness.createSubscription();

      const subscription = fakeNewSubscriptionEntity({ request: 'message' });
      expectApiSubscriptionsPostRequest(subscription, plan.id);
      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
    }));

    it('should not be able to subscribe to plan with general conditions', fakeAsync(async () => {
      await init({ ...APP, settings: { ...APP.settings, app: { client_id: 'client-id' } } });

      await dialogHarness.searchApi(API_NAME);
      tick(100);
      expectApiSearchPost(API);
      await dialogHarness.selectApi(API_NAME);

      const plan = fakePlanV4({
        name: 'JWT plan',
        apiId: API_ID,
        security: { type: 'JWT' },
        commentRequired: false,
        generalConditions: 'uuid',
      });
      expectSubscribableApiPlansGet([fakePlanV4({ apiId: API_ID, security: { type: 'API_KEY' } }), plan]);

      tick(100);
      expect(await dialogHarness.isPlanDisabled(plan.name)).toBeTruthy();
      expect(await dialogHarness.isCreateSubscriptionDisabled()).toBeTruthy();
    }));

    it('should create subscription for push plan', fakeAsync(async () => {
      await init();

      await dialogHarness.searchApi(API_NAME);
      tick(100);
      expectApiSearchPost(API);
      await dialogHarness.selectApi(API_NAME);

      const pushPlan = fakePlanV4({ apiId: API_ID, security: null, mode: 'PUSH', commentRequired: false, generalConditions: null });
      expectSubscribableApiPlansGet([pushPlan]);
      await dialogHarness.selectPlan(pushPlan.name);

      await dialogHarness.selectEntrypoint(WEBHOOK_ENTRYPOINT.name);
      expectSchemaGetRequest(WEBHOOK_ENTRYPOINT);

      await dialogHarness.createSubscription();
      const subscription = fakeNewSubscriptionEntity({ configuration: { entrypointId: WEBHOOK_ENTRYPOINT.id } });
      expectApiSubscriptionsPostRequest(subscription, pushPlan.id, null, 'PUSH');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['.', expect.anything()], expect.anything());
    }));
  });

  const expectSubscriptionsGetRequest = (
    subscriptions: SubscriptionPage[],
    size = '10',
    status = 'ACCEPTED,PAUSED,PENDING',
    securityTypes?: string,
  ) => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/subscriptions?page=1&size=${size}${
          status ? `&status=${status}` : ''
        }${securityTypes ? `&security_types=${securityTypes}` : ''}`,
        method: 'GET',
      })
      .flush(fakePagedResult(subscriptions));
    fixture.detectChanges();
  };

  const expectApiGetRequest = (api: Api) => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  };

  const expectApplicationGetRequest = (application: Application) => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(application);
  };

  const expectEntrypointListGet = () => {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' })
      .flush(ENTRYPOINT_LIST);
  };

  const expectApiSearchPost = (api: Api) => {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=9999&manageOnly=false`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({ query: 'my-api' });
    req.flush(fakePagedResult([api]));
  };

  const expectSubscribableApiPlansGet = (plans: Plan[] = []) => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&subscribableBy=${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(fakePagedResult(plans));
  };

  const expectApiSubscriptionsPostRequest = (
    subscription: NewSubscriptionEntity,
    planId: string,
    apiKeyMode: ApiKeyMode = undefined,
    planMode: string = undefined,
  ) => {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/subscriptions?plan=${planId}`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({
      request: subscription.request ?? '',
      ...(planMode === 'PUSH'
        ? {
            configuration: expect.objectContaining({
              entrypointId: subscription.configuration.entrypointId,
            }),
          }
        : {}),
      ...(apiKeyMode ? { apiKeyMode } : {}),
    });
    req.flush({ id: 'subscription-id' });
    flush();
    expectApplicationGetRequest(fakeApplication());
  };

  const expectSchemaGetRequest = (entrypoint: Partial<ConnectorPlugin>) => {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/${entrypoint.id}/subscription-schema`, method: 'GET' })
      .flush(getEntrypointConnectorSchema(entrypoint.id));
  };
});
