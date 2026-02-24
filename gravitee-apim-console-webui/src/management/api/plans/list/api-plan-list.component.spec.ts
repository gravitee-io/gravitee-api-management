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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { GioConfirmAndValidateDialogHarness } from '@gravitee/ui-particles-angular';
import { castArray, set } from 'lodash';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiPlanListComponent } from './api-plan-list.component';

import { ApiPlansModule } from '../api-plans.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Subscription } from '../../../../entities/subscription/subscription';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import {
  Api,
  ApiPlansResponse,
  fakeApiFederated,
  fakeApiV2,
  fakeApiV4,
  fakePlanFederated,
  fakePlanV2,
  fakePlanV4,
  fakeProxyApiV4,
  Plan,
  PLAN_STATUS,
} from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';

describe('ApiPlanListComponent', () => {
  const API_ID = 'api#1';
  const anAPi = fakeApiV2({ id: API_ID });

  let fixture: ComponentFixture<ApiPlanListComponent>;
  let component: ApiPlanListComponent;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [ApiPlansModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: ['api-plan-u', 'api-plan-r', 'api-plan-d'] },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          },
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              apikey: {
                enabled: true,
              },
              jwt: {
                enabled: true,
              },
              keyless: {
                enabled: true,
              },
              oauth2: {
                enabled: true,
              },
              customApiKey: {
                enabled: true,
              },
              sharedApiKey: {
                enabled: true,
              },
              push: {
                enabled: true,
              },
            });
            return constants;
          },
        },
      ],
    }).compileComponents();
  };

  beforeEach(async () => {
    await init();
  });

  afterEach(() => {
    jest.resetAllMocks();
    httpTestingController.verify();
  });

  describe('With a V2 API', () => {
    describe('plansTable tests', () => {
      it('should display an empty table', fakeAsync(async () => {
        await initComponent([]);

        const headerAndRowCells = await computePlansTableCells();
        expect(headerAndRowCells.headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);

        const table = await loader.getHarness(MatTableHarness);
        const tableHost = await table.host();
        expect(await tableHost.text()).toContain('There is no plan (yet).');
      }));

      it('should display a table with one row', fakeAsync(async () => {
        const plan = fakePlanV2({ tags: ['🙅', '🔑'] });
        await initComponent([plan]);

        const { headerCells, rowCells } = await computePlansTableCells();
        expect(headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);
        expect(rowCells).toEqual([['', 'Default plan', 'API Key', 'PUBLISHED', '🙅, 🔑', '']]);
      }));

      it('should not display PUSH plan option for V2 APIs', fakeAsync(async () => {
        await initComponent([]);
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());

        const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
        const items = await planSecurityDropdown.getItems();
        const availablePlans = await Promise.all(items.map(item => item.getText()));
        expect(availablePlans).not.toContain('Push plan');
      }));

      it('should display all plan options for V4 APIs with only HTTP and SUBSCRIPTION listeners', fakeAsync(async () => {
        const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP' }, { type: 'SUBSCRIPTION' }] });
        await initComponent([], v4Api);
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());

        const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
        const items = await planSecurityDropdown.getItems();
        const availablePlans = await Promise.all(items.map(item => item.getText()));
        expect(availablePlans).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)', 'Push plan']);
      }));

      it('should not display PUSH plan option for V4 APIs with only HTTP listeners', fakeAsync(async () => {
        const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP' }, { type: 'TCP' }] });
        await initComponent([], v4Api);
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());

        const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
        const items = await planSecurityDropdown.getItems();
        const availablePlans = await Promise.all(items.map(item => item.getText()));
        expect(availablePlans).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)']);
      }));

      it('should display only PUSH plan option for V4 APIs with only SUBSCRIPTION listeners', fakeAsync(async () => {
        const v4Api = fakeApiV4({ id: API_ID, listeners: [{ type: 'SUBSCRIPTION' }] });
        await initComponent([], v4Api);
        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());

        const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
        const items = await planSecurityDropdown.getItems();
        const availablePlans = await Promise.all(items.map(item => item.getText()));
        expect(availablePlans).toEqual(['Push plan']);
      }));

      it('should search closed plan on click', fakeAsync(async () => {
        const goldPlan = fakePlanV2({ name: 'gold plan ⭐️' });
        await initComponent([goldPlan]);

        await loader.getHarness(MatButtonToggleHarness.with({ text: /CLOSED/ })).then(btn => btn.toggle());

        const closedPlan = fakePlanV2({ name: 'closed plan 🚪', status: 'CLOSED' });
        expectApiPlansListRequest([closedPlan], 'CLOSED');

        const { rowCells } = await computePlansTableCells();
        expect(rowCells).toEqual([['', 'closed plan 🚪', 'API Key', 'CLOSED', 'tag1', '']]);
      }));

      it('should search and not find any plan', fakeAsync(async () => {
        await initComponent([fakePlanV2()]);

        await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());

        expectApiPlansListRequest([], 'STAGING');

        const table = await loader.getHarness(MatTableHarness);
        const tableHost = await table.host();
        expect(await tableHost.text()).toContain('There is no plan (yet).');
      }));
    });

    describe('drop tests', () => {
      it('should drop a plan and update his order', async () => {
        const plan1 = fakePlanV2({ name: 'Plan 1️⃣', order: 1 });
        const plan2 = fakePlanV2({ name: 'Plan 2️⃣', order: 2 });
        await initComponent([plan1, plan2]);

        component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

        expectApiPlanUpdateRequest({ ...plan2, order: 1 });
        expectApiGetRequest();
        expectApiPlansListRequest(
          [
            { ...plan2, order: 1 },
            { ...plan1, order: 2 },
          ],
          [...PLAN_STATUS],
        );
      });

      it('should fail to update and reload plans', async () => {
        const plan1 = fakePlanV2({ name: 'Plan 1️⃣', order: 1 });
        const plan2 = fakePlanV2({ name: 'Plan 2️⃣', order: 2 });
        await initComponent([plan1, plan2]);
        const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

        component.dropRow({ previousIndex: 1, currentIndex: 0 } as any);

        expectApiPlanUpdateRequestFail({ ...plan2, order: 1 });
        expectApiGetRequest();
        expectApiPlansListRequest(
          [
            { ...plan2, order: 1 },
            { ...plan1, order: 2 },
          ],
          [...PLAN_STATUS],
        );
        expect(snackBarSpy).toHaveBeenCalled();
      });
    });

    describe('actions tests', () => {
      describe('with Angular router', () => {
        it('should navigate to edit when click on the action button', async () => {
          await init();
          const plan = fakePlanV2();
          await initComponent([plan]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Edit the plan"]' })).then(btn => btn.click());

          expect(routerNavigateSpy).toBeCalledWith(['../plans'], expect.anything());
        });
      });

      it('should navigate to design when click on the design button', async () => {
        const plan = fakePlanV2();
        await initComponent([plan]);

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' })).then(btn => btn.click());

        expect(routerNavigateSpy).toBeCalledWith(['../v2/policy-studio'], {
          queryParams: { flows: `${plan.id}_0` },
          relativeTo: expect.anything(),
        });
      });

      it('should navigate to v4 policy studio when designPlan is called with V4 API', async () => {
        const v4Api = fakeApiV4({ id: API_ID });
        const plan = fakePlanV4({ apiId: API_ID, status: 'PUBLISHED' });
        await initComponent([plan], v4Api);

        component.designPlan(plan.id);

        expect(routerNavigateSpy).toBeCalledWith(['../v4/policy-studio'], {
          relativeTo: expect.anything(),
        });
      });

      describe('should publish the staging plan', () => {
        it('With a plan V2', async () => {
          const plan = fakePlanV2({ apiId: API_ID, name: 'publish me ☁️️', status: 'STAGING' });
          await initComponent([plan]);

          await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
          expectApiPlansListRequest([plan], 'STAGING');

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'publish me ☁️️', 'API Key', 'STAGING', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then(btn => btn.click());

          const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await confirmDialogSwitchButton.click();

          const updatedPlan: Plan = { ...plan, status: 'PUBLISHED' };
          expectApiPlanPublishRequest(updatedPlan);
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });

        it('With a plan V4', async () => {
          const plan = fakePlanV4({ apiId: API_ID, name: 'publish me ☁️️', status: 'STAGING' });
          await initComponent([plan]);

          await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
          expectApiPlansListRequest([plan], 'STAGING');

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'publish me ☁️️', 'API Key', 'STAGING', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' })).then(btn => btn.click());

          const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await confirmDialogSwitchButton.click();

          const updatedPlan: Plan = { ...plan, status: 'PUBLISHED' };
          expectApiPlanPublishRequest(updatedPlan);
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });
      });

      describe('should deprecate the published plan V2', () => {
        it('With a plan V2', async () => {
          const plan = fakePlanV2({ apiId: API_ID, name: 'deprecate me 😥️', status: 'PUBLISHED' });
          await initComponent([plan]);

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'deprecate me 😥️', 'API Key', 'PUBLISHED', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then(btn => btn.click());

          const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
          const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' }));
          await confirmDialogSwitchButton.click();

          const updatedPlan: Plan = { ...plan, status: 'DEPRECATED' };
          expectApiPlanDeprecateRequest(updatedPlan);
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });

        it('With a plan V4', async () => {
          const plan = fakePlanV4({ apiId: API_ID, name: 'deprecate me 😥️', status: 'PUBLISHED' });
          await initComponent([plan]);

          const table = await computePlansTableCells();
          expect(table.rowCells).toEqual([['', 'deprecate me 😥️', 'API Key', 'PUBLISHED', 'tag1', '']]);

          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Deprecate the plan"]' })).then(btn => btn.click());

          const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#deprecatePlanDialog' }));
          const confirmDialogSwitchButton = await confirmDialog.getHarness(MatButtonHarness.with({ text: 'Deprecate' }));
          await confirmDialogSwitchButton.click();

          const updatedPlan: Plan = { ...plan, status: 'DEPRECATED' };
          expectApiPlanDeprecateRequest(updatedPlan);
          expectApiGetRequest();
          expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
        });
      });

      describe('close plan', () => {
        describe('should close the published plan', () => {
          it('With a plan V2', async () => {
            const plan = fakePlanV2({ apiId: API_ID, name: 'close me 🚪️', status: 'PUBLISHED' });
            await initComponent([plan]);

            const table = await computePlansTableCells();
            expect(table.rowCells).toEqual([['', 'close me 🚪️', 'API Key', 'PUBLISHED', 'tag1', '']]);

            await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then(btn => btn.click());

            expectGetApiPlanSubscriptionsRequest(plan.id);

            const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
            expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Yes, close this plan.' }))).toBeTruthy();
            await confirmDialog.confirm();

            const updatedPlan: Plan = { ...plan, status: 'CLOSED' };
            expectApiPlanCloseRequest(updatedPlan);
            expectApiGetRequest();
            expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
          });

          it('With a plan V4', async () => {
            const plan = fakePlanV4({ apiId: API_ID, name: 'close me 🚪️', status: 'PUBLISHED' });
            await initComponent([plan]);

            const table = await computePlansTableCells();
            expect(table.rowCells).toEqual([['', 'close me 🚪️', 'API Key', 'PUBLISHED', 'tag1', '']]);

            await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Close the plan"]' })).then(btn => btn.click());

            expectGetApiPlanSubscriptionsRequest(plan.id);

            const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
            expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Yes, close this plan.' }))).toBeTruthy();
            await confirmDialog.confirm();

            const updatedPlan: Plan = { ...plan, status: 'CLOSED' };
            expectApiPlanCloseRequest(updatedPlan);
            expectApiGetRequest();
            expectApiPlansListRequest([updatedPlan], [...PLAN_STATUS]);
          });
        });
      });
    });
  });

  describe('With a V4 API', () => {
    const asyncApi = fakeApiV4({ id: API_ID });
    const anotherAsyncApi = fakeApiV4({
      id: API_ID,
      listeners: [
        {
          type: 'SUBSCRIPTION',
          entrypoints: [
            {
              type: 'webhook',
            },
          ],
        },
        {
          type: 'HTTP',
          entrypoints: [
            {
              type: 'http-proxy',
            },
          ],
        },
      ],
    });
    const httpProxyApi = fakeProxyApiV4({ id: API_ID });
    const tcpApi = fakeProxyApiV4({
      id: API_ID,
      listeners: [
        {
          type: 'TCP',
          hosts: ['foo.example.com', 'bar.example.com'],
          entrypoints: [
            {
              type: 'tcp-proxy',
            },
          ],
        },
      ],
    });

    const nativeApi = fakeApiV4({
      id: API_ID,
      type: 'NATIVE',
      listeners: [
        {
          type: 'KAFKA',
          host: 'kafka-host',
        },
      ],
    });

    describe('plansTable tests', () => {
      it('should display an empty table', fakeAsync(async () => {
        await initComponent([], asyncApi);

        const { headerCells } = await computePlansTableCells();
        expect(headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);

        const table = await loader.getHarness(MatTableHarness);
        const tableHost = await table.host();
        expect(await tableHost.text()).toContain('There is no plan (yet).');
      }));

      it('should display a table with one row', fakeAsync(async () => {
        const plan = fakePlanV2({ tags: ['🙅', '🔑'] });
        await initComponent([plan], asyncApi);

        const { headerCells, rowCells } = await computePlansTableCells();
        expect(headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);
        expect(rowCells).toEqual([['', 'Default plan', 'API Key', 'PUBLISHED', '🙅, 🔑', '']]);
      }));

      it.each`
        api                | expectedPlans
        ${tcpApi}          | ${['Keyless (public)']}
        ${asyncApi}        | ${['Push plan']}
        ${anotherAsyncApi} | ${['OAuth2', 'JWT', 'API Key', 'Keyless (public)', 'Push plan']}
        ${httpProxyApi}    | ${['OAuth2', 'JWT', 'API Key', 'Keyless (public)']}
        ${nativeApi}       | ${['OAuth2', 'JWT', 'API Key', 'Keyless (public)']}
      `(
        'should filter plans according to listener types',
        fakeAsync(async ({ api, expectedPlans }) => {
          await initComponent([], api);
          await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());

          const planSecurityDropdown = await loader.getHarness(MatMenuHarness);
          const items = await planSecurityDropdown.getItems();
          const availablePlans = await Promise.all(items.map(item => item.getText()));
          expect(availablePlans).toStrictEqual(expectedPlans);
        }),
      );
    });

    describe('Publish plan for Native Kafka API', () => {
      describe('Keyless plan in staging', () => {
        const KEYLESS_PLAN = fakePlanV4({ security: { type: 'KEY_LESS' }, status: 'STAGING' });
        beforeEach(async () => {
          await initComponent([KEYLESS_PLAN], nativeApi, 'STAGING');
        });

        it('should publish plan and close published plans with authentication', async () => {
          const { rowCells } = await computePlansTableCells();
          expect(rowCells).toHaveLength(1);

          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedApiKeyPlan = fakePlanV4({ id: 'api-key-plan', status: 'PUBLISHED', security: { type: 'API_KEY' } });
          const publishedOAuth2Plan = fakePlanV4({ id: 'oauth2-plan', status: 'PUBLISHED', security: { type: 'OAUTH2' } });

          expectApiPlansListRequest([publishedApiKeyPlan, publishedOAuth2Plan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          expect(await dialog.getText()).toContain('Are you sure you want to publish the Keyless plan Default plan?');

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Publish & Close' }))).toBeTruthy();
          await confirmDialog.confirm();

          expectApiPlanCloseRequest(publishedApiKeyPlan);
          expectApiPlanCloseRequest(publishedOAuth2Plan);
          expectApiPlanPublishRequest(KEYLESS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should send request to publish a Keyless plan even if Keyless plan already published', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          expectApiPlansListRequest([fakePlanV4({ security: { type: 'KEY_LESS' } })], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const publishBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await publishBtnInDialog.click();

          expectApiPlanPublishRequest(KEYLESS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should publish Keyless plan if no other plans are published', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          expectApiPlansListRequest([], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const publishBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await publishBtnInDialog.click();

          expectApiPlanPublishRequest(KEYLESS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should cancel publishing a keyless plan', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedApiKeyPlan = fakePlanV4({ id: 'api-key-plan', status: 'PUBLISHED', security: { type: 'API_KEY' } });
          const publishedOAuth2Plan = fakePlanV4({ id: 'oauth2-plan', status: 'PUBLISHED', security: { type: 'OAUTH2' } });

          expectApiPlansListRequest([publishedApiKeyPlan, publishedOAuth2Plan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          const cancelBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Cancel' }));
          await cancelBtnInDialog.click();
        });
      });

      describe('API Key plan in staging', () => {
        const API_KEY_PLAN = fakePlanV4({ security: { type: 'API_KEY' }, status: 'STAGING' });
        beforeEach(async () => {
          await initComponent([API_KEY_PLAN], nativeApi, 'STAGING');
        });

        it('should publish API Key plan and close published Keyless plan', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedKeylessPlan = fakePlanV4({ id: 'keyless-plan', status: 'PUBLISHED', security: { type: 'KEY_LESS' } });

          expectApiPlansListRequest([publishedKeylessPlan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          expect(await dialog.getText()).toContain('Are you sure you want to publish the authentication plan Default plan?');

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Publish & Close' }))).toBeTruthy();
          await confirmDialog.confirm();

          expectApiPlanCloseRequest(publishedKeylessPlan);
          expectApiPlanPublishRequest(API_KEY_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should publish an API Key plan if API Key plan already published', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedApiKeyPlan = fakePlanV4({ id: 'api-key-plan', status: 'PUBLISHED', security: { type: 'API_KEY' } });

          expectApiPlansListRequest([publishedApiKeyPlan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const publishBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await publishBtnInDialog.click();

          expectApiPlanPublishRequest(API_KEY_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should publish API Key plan if no plans already published', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          expectApiPlansListRequest([], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const publishBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await publishBtnInDialog.click();

          expectApiPlanPublishRequest(API_KEY_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should cancel publishing a plan with authentication', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedKeylessPlan = fakePlanV4({ id: 'keyless-plan', status: 'PUBLISHED', security: { type: 'KEY_LESS' } });

          expectApiPlansListRequest([publishedKeylessPlan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          const cancelBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Cancel' }));
          await cancelBtnInDialog.click();
        });
      });

      describe('MTLS plan in staging', () => {
        const MTLS_PLAN = fakePlanV4({ security: { type: 'MTLS' }, status: 'STAGING' });
        beforeEach(async () => {
          await initComponent([MTLS_PLAN], nativeApi, 'STAGING');
        });

        it('should publish MTLS plan and close published Keyless and auth plans', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedKeylessPlan = fakePlanV4({ id: 'keyless-plan', status: 'PUBLISHED', security: { type: 'KEY_LESS' } });
          const publishedAuthPlan = fakePlanV4({ id: 'auth-plan', status: 'PUBLISHED', security: { type: 'API_KEY' } });

          expectApiPlansListRequest([publishedKeylessPlan, publishedAuthPlan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          expect(await dialog.getText()).toContain('Are you sure you want to publish the mTLS plan');

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          expect(await rootLoader.getHarness(MatButtonHarness.with({ text: 'Publish & Close' }))).toBeTruthy();
          await confirmDialog.confirm();

          expectApiPlanCloseRequest(publishedKeylessPlan);
          expectApiPlanCloseRequest(publishedAuthPlan);
          expectApiPlanPublishRequest(MTLS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should publish MTLS plan and close published Keyless plan only', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          const publishedKeylessPlan = fakePlanV4({ id: 'keyless-plan', status: 'PUBLISHED', security: { type: 'KEY_LESS' } });

          expectApiPlansListRequest([publishedKeylessPlan], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness);
          expect(await dialog.getText()).toContain('Are you sure you want to publish the mTLS plan');

          const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
          await confirmDialog.confirm();

          expectApiPlanCloseRequest(publishedKeylessPlan);
          expectApiPlanPublishRequest(MTLS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });

        it('should publish MTLS plan directly if no Keyless or auth plans are published', async () => {
          const publishBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Publish the plan"]' }));
          await publishBtn.click();

          expectApiPlansListRequest([], ['PUBLISHED']);

          const dialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#publishPlanDialog' }));
          const publishBtnInDialog = await dialog.getHarness(MatButtonHarness.with({ text: 'Publish' }));
          await publishBtnInDialog.click();

          expectApiPlanPublishRequest(MTLS_PLAN);

          expectApiGetRequest(nativeApi);
          expectApiPlansListRequest([], [...PLAN_STATUS]);
        });
      });
    });
  });

  describe('With a Federated API', () => {
    const federatedApi = fakeApiFederated({ id: API_ID });

    describe('plansTable tests', () => {
      it('should display an empty table', fakeAsync(async () => {
        await initComponent([], federatedApi);

        const { headerCells } = await computePlansTableCells();
        expect(headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);

        const table = await loader.getHarness(MatTableHarness);
        const tableHost = await table.host();
        expect(await tableHost.text()).toContain('There is no plan (yet).');
      }));

      it('should display a table with plans', fakeAsync(async () => {
        const plan = fakePlanFederated();
        await initComponent([plan], federatedApi);

        const { headerCells, rowCells } = await computePlansTableCells();
        expect(headerCells).toEqual([
          {
            'drag-icon': '',
            name: 'Name',
            status: 'Status',
            'deploy-on': 'Deploy on',
            type: 'Type',
            actions: '',
          },
        ]);
        expect(rowCells).toEqual([['', 'Default plan', 'API Key', 'PUBLISHED', '', '']]);
      }));

      it('should hide create plan button', fakeAsync(async () => {
        const plan = fakePlanFederated();
        await initComponent([plan], federatedApi);

        const newPlanButton = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }));
        expect(newPlanButton).toBeNull();
      }));
    });
  });

  async function initComponent(plans: Plan[], api: Api = anAPi, activePlanStatusTab: string = 'PUBLISHED') {
    await TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { params: { apiId: api.id }, queryParams: { status: activePlanStatusTab } } },
    }).compileComponents();
    fixture = TestBed.createComponent(ApiPlanListComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();

    expectApiGetRequest(api);
    expectApiPlansListRequest(plans, [...PLAN_STATUS]);

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  }

  async function computePlansTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApiPlansListRequest(plans: Plan[] = [], statuses?: string | string[], security?: string) {
    const response: ApiPlansResponse = { data: plans };
    httpTestingController
      .expectOne(
        `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans?page=1&perPage=9999&${
          statuses ? `statuses=${castArray(statuses).join(',')}` : 'statuses=PUBLISHED'
        }${security ? `securities=${security}` : ''}&fields=-flow`,
        'GET',
      )
      .flush(response);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanUpdateRequestFail(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}`, 'PUT');
    expect(req.request.body).toEqual(plan);
    req.error(new ErrorEvent('error'), { status: 400 });
    fixture.detectChanges();
  }

  function expectApiPlanPublishRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_publish`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanDeprecateRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_deprecate`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiPlanCloseRequest(plan: Plan) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${plan.id}/_close`, 'POST');
    expect(req.request.body).toEqual({});
    req.flush(plan);
    fixture.detectChanges();
  }

  function expectApiGetRequest(api: Api = anAPi) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectGetApiPlanSubscriptionsRequest(planId: string, subscriptions: Subscription[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/subscriptions?plan=${planId}&status=accepted,pending,rejected,closed,paused`,
        method: 'GET',
      })
      .flush({
        data: subscriptions,
        metadata: {},
        page: {
          size: subscriptions.length,
        },
      });
    fixture.detectChanges();
  }
});
