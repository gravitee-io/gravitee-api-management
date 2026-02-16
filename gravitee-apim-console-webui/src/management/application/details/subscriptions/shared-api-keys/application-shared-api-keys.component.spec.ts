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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApplicationSharedApiKeysComponent } from './application-shared-api-keys.component';
import { ApplicationSharedApiKeysHarness } from './application-shared-api-keys.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeApplicationSubscriptionApiKey } from '../../../../../entities/subscription/ApplicationSubscriptionApiKey.fixture';
import { ApiKeyMode, Application } from '../../../../../entities/application/Application';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApplicationSharedApiKeysComponent', () => {
  let fixture: ComponentFixture<ApplicationSharedApiKeysComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApplicationSharedApiKeysHarness;
  const applicationId = 'applicationId';
  let rootLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationSharedApiKeysComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: {
                applicationId,
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
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationSharedApiKeysComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSharedApiKeysHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    expectApplicationGetRequest(fakeApplication({ id: applicationId, api_key_mode: ApiKeyMode.SHARED }));
    fixture.autoDetectChanges();
  });

  it('should display API keys', async () => {
    expectApplicationApiKeysGetRequest();

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

  it('should revoke API key', async () => {
    expectApplicationApiKeysGetRequest();

    const subscriptionApiKeysHarness = await componentHarness.getSubscriptionApiKeysHarness();

    await subscriptionApiKeysHarness.revokeApiKey();

    await rootLoader.getHarness(GioConfirmDialogHarness).then(dialog => dialog.confirm());

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys/1`,
      method: 'DELETE',
    });
  });

  it('should renew API key', async () => {
    expectApplicationApiKeysGetRequest();

    const subscriptionApiKeysHarness = await componentHarness.getSubscriptionApiKeysHarness();

    await subscriptionApiKeysHarness.renewApiKey();

    await rootLoader.getHarness(GioConfirmDialogHarness).then(dialog => dialog.confirm());

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys/_renew`,
      method: 'POST',
    });
  });

  const expectApplicationGetRequest = (application: Application): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}`,
        method: 'GET',
      })
      .flush(application);
  };

  const expectApplicationApiKeysGetRequest = (): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/apikeys`,
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
