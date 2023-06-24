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
import { CreateSubscription, Entrypoint, fakePlanV4, Plan } from "../../../../../../entities/management-api-v2";
import { MatDialog } from '@angular/material/dialog';
import {
  ApiPortalSubscriptionCreationDialogComponent,
  ApiPortalSubscriptionCreationDialogData,
  ApiPortalSubscriptionCreationDialogResult,
} from './api-portal-subscription-creation-dialog.component';
import { ApiPortalSubscriptionsModule } from '../../api-portal-subscriptions.module';
import { ApiPortalSubscriptionCreationDialogHarness } from './api-portal-subscription-creation-dialog.harness';

@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog" (click)="openDialog()">Open dialog</button>`,
})
class TestComponent {
  public plans?: Plan[];
  public subscriptionToCreate: CreateSubscription;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<
        ApiPortalSubscriptionCreationDialogComponent,
        ApiPortalSubscriptionCreationDialogData,
        ApiPortalSubscriptionCreationDialogResult
      >(ApiPortalSubscriptionCreationDialogComponent, {
        data: {
          plans: this.plans,
        },
        role: 'alertdialog',
        id: 'testDialog',
      })
      .afterClosed()
      .subscribe((result) => {
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

  describe('With custom apikey enabled', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule],
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
            useValue: {
              env: {
                settings: {
                  plan: {
                    security: {
                      customApiKey: {
                        enabled: true,
                      },
                    },
                  },
                },
              },
            },
          },
        ],
      });
      fixture = TestBed.createComponent(TestComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    describe('Test customApikey input', () => {
      it('should have input with API KEY Plan', async () => {
        const planV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'API_KEY' }, generalConditions: undefined });
        component.plans = [planV4];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(planV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeTruthy();
      });
      it('should not have input with OAUTH2 Plan', async () => {
        const planV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'OAUTH2' }, generalConditions: undefined });
        component.plans = [planV4];
        await componentTestingOpenDialog();

        const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

        await harness.choosePlan(planV4.name);

        expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();
      });
    });
  });

  describe('With custom apikey disabled', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [ApiPortalSubscriptionsModule, NoopAnimationsModule],
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
            useValue: {
              env: {
                settings: {
                  plan: {
                    security: {
                      customApiKey: {
                        enabled: false,
                      },
                    },
                  },
                },
              },
            },
          },
        ],
      });
      fixture = TestBed.createComponent(TestComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('should not have input with API KEY Plan', async () => {
      const planV4 = fakePlanV4({ mode: 'STANDARD', security: { type: 'API_KEY' }, generalConditions: undefined });
      component.plans = [planV4];
      await componentTestingOpenDialog();

      const harness = await loader.getHarness(ApiPortalSubscriptionCreationDialogHarness);
      expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();

      await harness.choosePlan(planV4.name);

      expect(await harness.isCustomApiKeyInputDisplayed()).toBeFalsy();
    });
  });

  async function componentTestingOpenDialog() {
    const openDialogButton = await loader.getHarness(MatButtonHarness);
    await openDialogButton.click();
    fixture.detectChanges();
  }
});
