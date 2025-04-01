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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Component } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { SubscriptionApiKeysHarness } from './subscription-api-keys.harness';
import { SubscriptionApiKeysComponent } from './subscription-api-keys.component';

import { GioTestingPermissionProvider } from '../../../../../../shared/components/gio-permission/gio-permission.service';
import { fakeApplicationSubscriptionApiKey } from '../../../../../../entities/subscription/ApplicationSubscriptionApiKey.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';

@Component({
  selector: 'test-component',
  template: ` <subscription-api-keys
    [applicationId]="applicationId"
    [subscriptionId]="subscriptionId"
    [readonly]="readonly"
  ></subscription-api-keys>`,
  standalone: false,
})
class TestComponent {
  applicationId = 'applicationId';
  subscriptionId = 'subscriptionId';
  readonly = false;
}

describe('SubscriptionApiKeysComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let componentHarness: SubscriptionApiKeysHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [MatIconTestingModule, NoopAnimationsModule, SubscriptionApiKeysComponent, GioTestingModule],
      providers: [
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

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await loader.getHarness(SubscriptionApiKeysHarness);

    expectApplicationApiKeysGetRequest();
    fixture.autoDetectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display table', async () => {
    const { headerCells, rowCells } = await componentHarness.computeTableCells();
    expect(headerCells).toStrictEqual([
      {
        actions: '',
        'active-icon': '',
        createdAt: 'Created at',
        endDate: 'Revoked/Expired at',
        key: 'Key',
      },
    ]);

    expect(rowCells).toEqual([
      {
        activeIcon: 'check-circled-outline',
        key: 'key1',
        createdAt: 'Mar 21, 2024, 11:24:34 AM',
        endDate: '-',
        actions: 'hasRevokeButton',
      },
      {
        activeIcon: 'check-circled-outline',
        key: 'key4',
        createdAt: 'Mar 21, 2024, 11:24:34 AM',
        endDate: '-',
        actions: 'hasRevokeButton',
      },
      {
        activeIcon: 'x-circle',
        key: 'key2',
        createdAt: 'Mar 21, 2024, 11:24:34 AM',
        endDate: 'Apr 2, 2024, 12:48:38 PM',
        actions: undefined,
      },
      {
        activeIcon: 'x-circle',
        key: 'key3',
        createdAt: 'Mar 21, 2024, 11:24:34 AM',
        endDate: 'Apr 2, 2024, 12:48:38 PM',
        actions: undefined,
      },
    ]);
  });

  it('should revoke API key', async () => {
    await componentHarness.revokeApiKey();

    await rootLoader.getHarness(GioConfirmDialogHarness).then((dialog) => dialog.confirm());

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/subscriptions/subscriptionId/apikeys/1`,
      method: 'DELETE',
    });
  });

  it('should renew API key', async () => {
    await componentHarness.renewApiKey();

    await rootLoader.getHarness(GioConfirmDialogHarness).then((dialog) => dialog.confirm());

    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/applicationId/subscriptions/subscriptionId/apikeys/_renew`,
      method: 'POST',
    });
  });

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
        fakeApplicationSubscriptionApiKey({
          id: '2',
          key: 'key2',
          expired: true,
          expire_at: 1712062118650,
        }),
        fakeApplicationSubscriptionApiKey({
          id: '3',
          key: 'key3',
          revoked: true,
          revoked_at: 1712062118650,
        }),
        fakeApplicationSubscriptionApiKey({
          id: '4',
          key: 'key4',
        }),
      ]);
  };
});
