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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { HttpTestingController } from '@angular/common/http/testing';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';

import { ApiPortalSubscriptionCreationDialogHarness } from './api-portal-subscription-creation-dialog.harness';
import {
  ApiPortalSubscriptionCreationDialogComponent,
  ApiPortalSubscriptionCreationDialogData,
  ApiPortalSubscriptionCreationDialogResult,
} from './api-portal-subscription-creation-dialog.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../../shared/testing';
import { ApiPortalSubscriptionsModule } from '../../../api-portal-subscriptions.module';
import {
  ConnectorPlugin,
  CreateSubscription,
  Entrypoint,
  entrypointsGetResponse,
  fakeApiV4,
  fakePlanV4,
  Plan,
  VerifySubscription,
} from '../../../../../../../entities/management-api-v2';
import { Application } from '../../../../../../../entities/application/application';
import { PagedResult } from '../../../../../../../entities/pagedResult';
import { fakeApplication } from '../../../../../../../entities/application/Application.fixture';

@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog" (click)="openDialog()">Open dialog</button>`,
})
class TestComponent {
  public plans?: Plan[];
  public availableSubscriptionEntrypoints?: Entrypoint[];
  public subscriptionToCreate: CreateSubscription;
  public dialog: MatDialogRef<ApiPortalSubscriptionCreationDialogComponent>;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.dialog = this.matDialog.open<
      ApiPortalSubscriptionCreationDialogComponent,
      ApiPortalSubscriptionCreationDialogData,
      ApiPortalSubscriptionCreationDialogResult
    >(ApiPortalSubscriptionCreationDialogComponent, {
      data: {
        plans: this.plans,
        availableSubscriptionEntrypoints: this.availableSubscriptionEntrypoints,
      },
      role: 'alertdialog',
      id: 'testDialog',
    });

    this.dialog.afterClosed().subscribe((result) => {
      if (result) {
        this.subscriptionToCreate = result.subscriptionToCreate;
      }
    });
  }
}

describe('Subscription creation dialog', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  describe('Test customApikey input', () => {
    describe('With custom apikey enabled', () => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          declarations: [TestComponent],
          imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
          providers: [
            {
              provide: InteractivityChecker,
              useValue: {
                isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
                isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
              },
            },
            {
              provide: 'Constants',
              useFactory: () => {
                const constants = CONSTANTS_TESTING;
                set(constants, 'env.settings.plan.security', {
                  customApiKey: { enabled: true },
                });
                return constants;
              },
            },
          ],
        });
        fixture = TestBed.createComponent(TestComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
        fixture.detectChanges();
        component = fixture.componentInstance;
        loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      afterEach(() => {
        jest.clearAllMocks();
        httpTestingController.verify();
      });

      it('should have input with API Key Plan', async () => {
        const applicationWithClientId = fakeApplication({
          id: 'my-app',
          name: 'withClientId',
          settings: { app: { client_id: 'clientId' } },
        });
        const planV4 = fakePlanV4({ apiId: 'my-api', mode: 'STANDARD', security: { type: 'API_KEY' }, generalConditions: undefined });
        component.plans = [planV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(planV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.searchApplication('withClientId');
        expectApplicationsSearch('withClientId', [applicationWithClientId]);
        await harness.selectApplication(applicationWithClientId.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeTruthy();

        await harness.addCustomKey('12345678');
        const req = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/my-api/subscriptions/_verify`,
          method: 'POST',
        });
        const verifySubscription: VerifySubscription = {
          applicationId: 'my-app',
          apiKey: '12345678',
        };
        expect(req.request.body).toEqual(verifySubscription);
        req.flush({ ok: true });
      });
      it('should have input when selecting non API Key plan and then select an API Key Plan', async () => {
        const applicationWithClientId = fakeApplication({ name: 'withClientId', settings: { app: { client_id: 'clientId' } } });

        const apiKeyPlanV4 = fakePlanV4({
          name: 'API Key plan',
          mode: 'STANDARD',
          security: { type: 'API_KEY' },
          generalConditions: undefined,
        });
        const jwtPlanV4 = fakePlanV4({ name: 'JWT Plan', mode: 'STANDARD', security: { type: 'JWT' }, generalConditions: undefined });
        component.plans = [apiKeyPlanV4, jwtPlanV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);

        await harness.searchApplication('withClientId');
        expectApplicationsSearch('withClientId', [applicationWithClientId]);
        await harness.selectApplication(applicationWithClientId.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(jwtPlanV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(apiKeyPlanV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeTruthy();
      });
      it('should not have input with OAUTH2 Plan', async () => {
        const planV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'OAUTH2' }, generalConditions: undefined });
        component.plans = [planV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(planV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();
      });
    });
    describe('With custom apikey disabled', () => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          declarations: [TestComponent],
          imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
          providers: [
            {
              provide: InteractivityChecker,
              useValue: {
                isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
                isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
              },
            },
            {
              provide: 'Constants',
              useFactory: () => {
                const constants = CONSTANTS_TESTING;
                set(constants, 'env.settings.plan.security', {
                  customApiKey: { enabled: false },
                });
                return constants;
              },
            },
          ],
        });
        fixture = TestBed.createComponent(TestComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
        fixture.detectChanges();
        component = fixture.componentInstance;
        loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      afterEach(() => {
        jest.clearAllMocks();
        httpTestingController.verify();
      });

      it('should not have input with API Key Plan', async () => {
        const planV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'API_KEY' }, generalConditions: undefined });
        component.plans = [planV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(planV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();
      });
    });
  });

  describe('Test Push plan form', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
        providers: [
          {
            provide: InteractivityChecker,
            useValue: {
              isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
              isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
            },
          },
        ],
      });
      fixture = TestBed.createComponent(TestComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
      httpTestingController.verify();
    });

    it('should have Push Plan configuration form', async () => {
      const planV4 = fakePlanV4({ mode: 'PUSH', generalConditions: undefined });
      const apiV4 = fakeApiV4({ listeners: [{ type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] }] });
      component.plans = [planV4];
      component.availableSubscriptionEntrypoints = apiV4.listeners[0].entrypoints;
      await componentTestingOpenDialog();
      expectListEntrypoints(entrypointsGetResponse);

      const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
      expect(await harness.isChannelInputDisplayed()).toBeFalsy();
      expect(await harness.isEntrypointSelectDisplayed()).toBeFalsy();
      expect(await harness.isEntrypointConfigurationFormDisplayed()).toBeFalsy();

      await harness.choosePlan(planV4.name);

      expect(await harness.isChannelInputDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointSelectDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointConfigurationFormDisplayed()).toBeFalsy();

      await harness.selectEntrypoint('Webhook');
      expectEntrypointSubscriptionSchema('webhook');

      expect(await harness.isChannelInputDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointSelectDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointConfigurationFormDisplayed()).toBeTruthy();
    });

    it('should remove Push Plan configuration form when select another plan', async () => {
      const pushPlanV4 = fakePlanV4({ name: 'push plan', mode: 'PUSH', generalConditions: undefined });
      const jwtPlanV4 = fakePlanV4({ name: 'JWT plan', mode: 'STANDARD', security: { type: 'JWT' }, generalConditions: undefined });
      const apiV4 = fakeApiV4({ listeners: [{ type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] }] });
      component.plans = [pushPlanV4, jwtPlanV4];
      component.availableSubscriptionEntrypoints = apiV4.listeners[0].entrypoints;
      await componentTestingOpenDialog();
      expectListEntrypoints(entrypointsGetResponse);

      const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
      await harness.choosePlan(pushPlanV4.name);
      await harness.selectEntrypoint('Webhook');
      expectEntrypointSubscriptionSchema('webhook');

      expect(await harness.isChannelInputDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointSelectDisplayed()).toBeTruthy();
      expect(await harness.isEntrypointConfigurationFormDisplayed()).toBeTruthy();

      await harness.choosePlan(jwtPlanV4.name);
      expect(await harness.isChannelInputDisplayed()).toBeFalsy();
      expect(await harness.isEntrypointSelectDisplayed()).toBeFalsy();
      expect(await harness.isEntrypointConfigurationFormDisplayed()).toBeFalsy();
    });
  });

  describe('Test JWT & OAuth2 plans', () => {
    describe('Test clientId', () => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          declarations: [TestComponent],
          imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule],
          providers: [
            {
              provide: InteractivityChecker,
              useValue: {
                isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
                isTabbable: () => true, // This traps tabbable checks and so avoid warnings when dealing with
              },
            },
          ],
        });
        fixture = TestBed.createComponent(TestComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
        fixture.detectChanges();
        component = fixture.componentInstance;
        loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      afterEach(() => {
        jest.clearAllMocks();
        httpTestingController.verify();
      });

      it('Should not have error if application has clientId', async () => {
        const applicationWithClientId = fakeApplication({ name: 'withClientId', settings: { app: { client_id: 'clientId' } } });
        const jwtPlanV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'JWT' }, generalConditions: undefined });
        component.plans = [jwtPlanV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);

        await harness.searchApplication('withClientId');
        expectApplicationsSearch('withClientId', [applicationWithClientId]);
        await harness.selectApplication(applicationWithClientId.name);

        await harness.choosePlan(jwtPlanV4.name);

        const errors = fixture.componentInstance.dialog.componentInstance.form.get('selectedApplication').errors;
        expect(errors).toBeNull();

        expect(await (await harness.getCreateButton()).isDisabled()).toBeFalsy();
      });

      it('Should have error if application has no clientId', async () => {
        const applicationWithoutClientId = fakeApplication({ name: 'withoutClientId', settings: { app: {} } });
        const jwtPlanV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'JWT' }, generalConditions: undefined });
        component.plans = [jwtPlanV4];
        component.availableSubscriptionEntrypoints = [];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);

        await harness.searchApplication('withoutClientId');
        expectApplicationsSearch('withoutClientId', [applicationWithoutClientId]);
        await harness.selectApplication(applicationWithoutClientId.name);

        await harness.choosePlan(jwtPlanV4.name);

        const errors = fixture.componentInstance.dialog.componentInstance.form.get('selectedApplication').errors;
        expect(errors).toEqual({ clientIdRequired: true });

        expect(await (await harness.getCreateButton()).isDisabled()).toBeTruthy();
      });
    });
  });

  async function componentTestingOpenDialog() {
    const openDialogButton = await loader.getHarness(MatButtonHarness);
    await openDialogButton.click();
    fixture.detectChanges();
  }

  function expectListEntrypoints(entrypoints: ConnectorPlugin[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints`,
        method: 'GET',
      })
      .flush(entrypoints);
    fixture.detectChanges();
  }

  function expectEntrypointSubscriptionSchema(entrypointId: string) {
    const response: GioJsonSchema = {
      $schema: 'http://json-schema.org/draft-07/schema#',
      type: 'object',
      properties: {
        foo: {
          type: 'string',
        },
      },
    };

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints/${entrypointId}/subscription-schema`,
        method: 'GET',
      })
      .flush(response);
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
