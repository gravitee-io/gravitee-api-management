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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';
import { ApiEndpointGroupHarness } from './api-endpoint-group.harness';
import { ApiEndpointGroupModule } from './api-endpoint-group.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../../../entities/user';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

describe('ApiEndpointsGroupsComponent', () => {
  const fakeUiRouter = { go: jest.fn() };

  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const VALID_ENDPOINT_GROUP_NAME = 'New Endpoint Group Name';
  const INVALID_ENDPOINT_GROUP_NAME = '';
  const DEFAULT_LOAD_BALANCER_TYPE = 'ROUND_ROBIN';

  let fixture: ComponentFixture<ApiEndpointGroupComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiEndpointGroupHarness;

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  const initComponent = async () => {
    const currentUser = new DeprecatedUser();

    currentUser.userPermissions = ['api-definition-u', 'api-definition-c', 'api-definition-r'];

    const routerParams: unknown = { apiId: API_ID, groupIndex: 0 };

    const api = fakeApiV4({
      id: API_ID,
    });

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointGroupModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: routerParams },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: SnackBarService, useValue: SnackBarService },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiEndpointGroupComponent);

    httpTestingController = TestBed.inject(HttpTestingController);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupHarness);

    expectApiGetRequest(api);

    fixture.detectChanges();
  };

  beforeEach(async () => {
    await initComponent();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('GIVEN the current page is the endpoint group page', () => {
    let api: ApiV4;

    beforeEach(async () => {
      await TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiEndpointGroupComponent);
      httpTestingController = TestBed.inject(HttpTestingController);

      api = fakeApiV4({
        id: API_ID,
        endpointGroups: [
          {
            name: DEFAULT_GROUP_NAME,
            type: 'kafka',
            endpoints: [],
          },
        ],
      });
      fixture.detectChanges();
    });
    describe('WHEN the general tab is selected', () => {
      beforeEach(async () => {
        await componentHarness.clickGeneralTab();

        expectApiGetRequest(api);

        fixture.detectChanges();
      });
      it('THEN the endpoint group name field should be visible with a default value', async () => {
        expect(await componentHarness.readEndpointGroupNameInput()).toEqual(DEFAULT_GROUP_NAME);
      });

      it('THEN the endpoint group load balancer field should be visible with a default value', async () => {
        expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual(DEFAULT_LOAD_BALANCER_TYPE);
      });


      describe('AND WHEN the endpoint group name is modified with a valid value', () => {
        beforeEach(async () => {
          await componentHarness.writeToEndpointGroupNameInput(VALID_ENDPOINT_GROUP_NAME);

          fixture.detectChanges();
        });
        it('THEN the endpoint group name field should be updated with the expected value', async () => {
          expect(await componentHarness.readEndpointGroupNameInput()).toEqual(VALID_ENDPOINT_GROUP_NAME);
        });

        it('THEN the save button should be valid', async () => {
          expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();
        });

        describe('AND WHEN the save button is clicked', () => {
          beforeEach(async () => {
            api = fakeApiV4({
              id: API_ID,
              endpointGroups: [
                {
                  name: DEFAULT_GROUP_NAME,
                  type: 'kafka',
                  endpoints: [],
                },
              ],
            });

            await componentHarness.clickEndpointGroupSaveButton();

            expectApiGetRequest(api);

            fixture.detectChanges();
          });
          it('THEN the endpoint group name field should be saved', async () => {
            const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

            expect(req.request.body.endpointGroups).toEqual([
              {
                endpoints: [
                  {
                    configuration: { bootstrapServers: 'localhost:9092' },
                    inheritConfiguration: false,
                    name: 'default',
                    type: 'kafka',
                    weight: 1,
                  },
                ],
                loadBalancer: { type: 'ROUND_ROBIN' },
                name: VALID_ENDPOINT_GROUP_NAME,
                services: undefined,
                sharedConfiguration: undefined,
                type: 'kafka',
              },
            ]);
          });
        });

        describe('AND WHEN the changes made have been dismissed', () => {
          beforeEach(async () => {
            await componentHarness.writeToEndpointGroupNameInput('TEST');

            fixture.detectChanges();
          });

          it('THEN the endpoint group details should reset back to the original values', async () => {
            await componentHarness.clickEndpointGroupDismissButton();

            expectApiGetRequest(api);

            expect(await componentHarness.readEndpointGroupNameInput()).toEqual(DEFAULT_GROUP_NAME);
          });
        });

        describe('AND WHEN the endpoint group is modified with an invalid value', () => {
          beforeEach(async () => {
            await componentHarness.writeToEndpointGroupNameInput(INVALID_ENDPOINT_GROUP_NAME);

            fixture.detectChanges();
          });

          it('THEN the save button should be invalid', async () => {
            expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeTruthy();
          });
        });
      });
    });

    describe('WHEN the back button is clicked', () => {
      it('THEN the endpoint groups list page should be the next page expected to be shown', async () => {
        const routerSpy = jest.spyOn(fakeUiRouter, 'go');
        api = fakeApiV4({
          id: API_ID,
        });

        await componentHarness.clickBackButton();

        expectApiGetRequest(api);

        expect(routerSpy).toHaveBeenCalledWith('management.apis.ng.endpoints', undefined, undefined);
      });
    });
  });
});
