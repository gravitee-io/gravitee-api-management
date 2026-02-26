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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApplicationSubscriptionComponent } from './application-subscription.component';
import { ApplicationSubscriptionHarness } from './application-subscription.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeSubscription } from '../../../../../entities/subscription/subscription.fixture';
import { Subscription } from '../../../../../entities/subscription/subscription';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Application } from '../../../../../entities/application/Application';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { fakeApplicationSubscriptionApiKey } from '../../../../../entities/subscription/ApplicationSubscriptionApiKey.fixture';
import { SubscriptionEditPushConfigHarness } from '../../../../../components/subscription-edit-push-config/subscription-edit-push-config.harness';
import { SubscriptionEditPushConfigDialogHarness } from '../../../../../components/subscription-edit-push-config-dialog/subscription-edit-push-config-dialog.harness';

describe('ApplicationSubscriptionComponent', () => {
  let fixture: ComponentFixture<ApplicationSubscriptionComponent>;
  let componentHarness: ApplicationSubscriptionHarness;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  const applicationId = 'applicationId';
  const subscriptionId = 'subscriptionId';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationSubscriptionComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                applicationId,
                subscriptionId,
              },
            },
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'application-subscription-c',
            'application-subscription-r',
            'application-subscription-u',
            'application-subscription-d',
          ],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationSubscriptionComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSubscriptionHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.autoDetectChanges();
    expectApplicationGetRequest(fakeApplication({ id: applicationId }));
    expectApplicationSubscriptionGet(
      applicationId,
      fakeSubscription({
        id: subscriptionId,
        plan: {
          id: 'planId',
          name: 'Free Spaceshuttle',
          security: 'API_KEY',
        },
      }),
    );
    fixture.detectChanges();
    expectApplicationApiKeysGetRequest();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display subscription details', async () => {
    expect(await componentHarness.getSubscriptionDetails()).toEqual([
      ['ID', 'subscriptionId content_copy'],
      ['API', '🪐 Planets - 1.0 content_copy'],
      ['Plan', 'Free Spaceshuttle content_copy'],
      ['Status', 'ACCEPTED content_copy'],
      ['Subscribed by', 'Bruce Wayne content_copy'],
      ['Created at', expect.any(String)],
      ['Processed at', expect.any(String)],
      ['Starting at', expect.any(String)],
      ['Paused at', '-'],
      ['Ending at', '-'],
      ['Closed at', '-'],
      ['Metadata', '-'],
    ]);

    const subscriptionApiKeysHarness = await componentHarness.getSubscriptionApiKeysHarness();
    expect((await subscriptionApiKeysHarness.computeTableCells()).rowCells).toEqual([
      {
        activeIcon: 'check-circled-outline',
        key: 'key1',
        createdAt: 'Mar 21, 2024, 11:24:34 AM',
        endDate: '-',
        actions: 'hasRevokeButton',
      },
    ]);
  });

  describe('metadata display', () => {
    it('should show dash when subscription has no metadata', async () => {
      expect(await componentHarness.getMetadata()).toEqual('-');
      expect(await componentHarness.metadataEditorIsVisible()).toEqual(false);
    });

    it('should show monaco editor when subscription has metadata', async () => {
      const subscriptionWithMetadata = fakeSubscription({
        id: subscriptionId,
        plan: { id: 'planId', name: 'Free Spaceshuttle', security: 'API_KEY' },
        metadata: { env: 'prod', version: '1' },
      });

      fixture = TestBed.createComponent(ApplicationSubscriptionComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
      componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSubscriptionHarness);
      fixture.autoDetectChanges();
      expectApplicationSubscriptionGet(applicationId, subscriptionWithMetadata);
      fixture.detectChanges();
      expectApplicationApiKeysGetRequest();

      expect(await componentHarness.metadataEditorIsVisible()).toEqual(true);
    });
  });

  it('should update consumer subscription configuration', async () => {
    const subscription = fakeSubscription({
      id: subscriptionId,
      plan: {
        id: 'planId',
        name: 'Push plan',
      },
      configuration: {
        entrypointId: 'webhook',
        channel: 'myChannel',
        entrypointConfiguration: {
          callbackUrl: 'https://webhook.site/296b8f8b-fbbe-4516-a016-c44da934b5e0',
        },
      },
    });

    fixture = TestBed.createComponent(ApplicationSubscriptionComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSubscriptionHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.autoDetectChanges();
    expectApplicationSubscriptionGet(applicationId, subscription);
    fixture.detectChanges();

    const apiSubscriptionEditPushConfigCard = await rootLoader.getHarness(SubscriptionEditPushConfigHarness);
    expect(await apiSubscriptionEditPushConfigCard.getContentText()).toContain('https://webhook.site/296b8f8b-fbbe-4516-a016-c44da934b5e0');
    expect(await apiSubscriptionEditPushConfigCard.getContentText()).toContain('myChannel');

    await apiSubscriptionEditPushConfigCard.clickOpenConfigurationButton();

    const apiSubscriptionEditPushConfigDialog = await rootLoader.getHarness(SubscriptionEditPushConfigDialogHarness);

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/webhook/subscription-schema`,
        method: 'GET',
      })
      .flush({ type: 'object', properties: { callbackUrl: { type: 'string' } } });

    expect(await apiSubscriptionEditPushConfigDialog.getChannelValue()).toEqual('myChannel');

    await apiSubscriptionEditPushConfigDialog.setChannelInput('newChannel');
    await apiSubscriptionEditPushConfigDialog.save();

    expectApplicationSubscriptionGet(applicationId, subscription);

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}`,
      method: 'PUT',
    });
    expect(JSON.stringify(req.request.body)).toEqual(
      JSON.stringify({
        ...subscription,
        configuration: {
          ...subscription.configuration,
          channel: 'newChannel',
        },
      }),
    );
  });

  it('should close subscription', async () => {
    await componentHarness.closeSubscription();

    const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
    await confirmDialog.confirm();

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/subscriptions/${subscriptionId}`,
      method: 'DELETE',
    });
  });

  const expectApplicationGetRequest = (application: Application): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}`,
        method: 'GET',
      })
      .flush(application);
  };

  function expectApplicationSubscriptionGet(applicationId: string, subscription: Subscription): void {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${applicationId}/subscriptions/${subscription.id}`,
        method: 'GET',
      })
      .flush(subscription);
  }

  const expectApplicationApiKeysGetRequest = (): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/subscriptions/subscriptionId/apikeys`,
        method: 'GET',
      })
      .flush([
        fakeApplicationSubscriptionApiKey({
          id: '1',
          key: 'key1',
        }),
      ]);
  };
});
