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
          useValue: ['application-subscription-c', 'application-subscription-r', 'application-subscription-d'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationSubscriptionComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSubscriptionHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.autoDetectChanges();
    expectApplicationSubscriptionGet(
      applicationId,
      fakeSubscription({
        id: subscriptionId,
      }),
    );
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
      ['Paused at', ''],
      ['Ending at', ''],
      ['Closed at', ''],
    ]);
  });

  it('should close subscription', async () => {
    await componentHarness.closeSubscription();

    expectApplicationGetRequest(fakeApplication({ id: applicationId }));

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
});
