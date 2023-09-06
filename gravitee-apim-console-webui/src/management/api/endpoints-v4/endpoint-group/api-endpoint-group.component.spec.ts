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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { FormsModule } from '@angular/forms';

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';
import { ApiEndpointGroupHarness } from './api-endpoint-group.harness';
import { ApiEndpointGroupModule } from './api-endpoint-group.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../../entities/user';
import { ApiV4, EndpointGroupV4, fakeApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { fakeEndpointGroupV4 } from '../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';

/**
 * Test data
 */
const API_ID = 'apiId';
const DEFAULT_GROUP_NAME = 'default-group';
const VALID_ENDPOINT_GROUP_NAME = 'New Endpoint Group Name';
const INVALID_ENDPOINT_GROUP_NAME = '';
const DEFAULT_LOAD_BALANCER_TYPE = 'ROUND_ROBIN';
const ALTERNATE_LOAD_BALANCER_TYPE = 'RANDOM';
const ALTERNATE_LOAD_BALANCER_TYPE_2 = 'WEIGHTED_RANDOM';
const GROUP_INDEX = 0;
const FAKE_UI_ROUTER = { go: jest.fn() };

/**
 * Given the api and the updated endpoint group attributes
 * return the newly modified endpoint group
 *
 * @param api api object
 * @param endpointGroupAttributes endpoint group attributes that will be updated
 */
function getExpectedEndpoints(api: ApiV4, endpointGroupAttributes): EndpointGroupV4[] {
  return [{ ...api.endpointGroups[GROUP_INDEX], ...endpointGroupAttributes }];
}

/**
 * Expect that a single GET request has been made which matches the specified URL
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiGetRequest(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController) {
  httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  fixture.detectChanges();
}

/**
 * Search for API Schema GET requests that match the specified parameter
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiSchemaGetRequests(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController) {
  httpTestingController.match({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/kafka/shared-configuration-schema`, method: 'GET' });
  fixture.detectChanges();
}

/**
 * Expect that a single PUT request has been made which matches the specified URL
 *
 * @param api api object
 * @param fixture testing fixture
 * @param httpTestingController http testing controller
 */
function expectApiPutRequest(api: ApiV4, fixture: ComponentFixture<any>, httpTestingController: HttpTestingController): TestRequest {
  return httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
}

describe('ApiEndpointGroupComponent', () => {
  let fixture: ComponentFixture<ApiEndpointGroupComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiEndpointGroupHarness;
  let api: ApiV4;

  const initComponent = async (testApi: ApiV4) => {
    const currentUser = new DeprecatedUser();

    currentUser.userPermissions = ['api-definition-u', 'api-definition-c', 'api-definition-r'];

    const routerParams: unknown = { apiId: API_ID, groupIndex: GROUP_INDEX };

    api = testApi;

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointGroupModule, MatIconTestingModule, FormsModule],
      providers: [
        { provide: UIRouterState, useValue: FAKE_UI_ROUTER },
        { provide: UIRouterStateParams, useValue: routerParams },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: SnackBarService, useValue: SnackBarService },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiEndpointGroupComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupHarness);

    expectApiGetRequest(api, fixture, httpTestingController);

    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('GIVEN the current page is the endpoint group page', () => {
    beforeEach(async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
    });

    // General Tab
    describe('WHEN the general tab is selected', () => {
      beforeEach(async () => {
        await componentHarness.clickGeneralTab();
        expectApiSchemaGetRequests(api, fixture, httpTestingController);
      });

      // General Tab -> Endpoint Group Name
      it('THEN the endpoint group name field should be visible with a default value', async () => {
        expect(await componentHarness.readEndpointGroupNameInput()).toEqual(DEFAULT_GROUP_NAME);
      });

      describe('AND WHEN the endpoint group name is modified with a valid value', () => {
        beforeEach(async () => {
          await componentHarness.writeToEndpointGroupNameInput(VALID_ENDPOINT_GROUP_NAME);
        });
        it('THEN the endpoint group name field should be updated with the expected value', async () => {
          expect(await componentHarness.readEndpointGroupNameInput()).toEqual(VALID_ENDPOINT_GROUP_NAME);
        });

        it('THEN the save button should be valid', async () => {
          expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();
        });

        describe('AND WHEN the save button is clicked', () => {
          it('THEN the endpoint group name field should be saved', async () => {
            await componentHarness.clickEndpointGroupSaveButton();
            expectApiGetRequest(api, fixture, httpTestingController);
            expect(expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups).toEqual(
              getExpectedEndpoints(api, { name: VALID_ENDPOINT_GROUP_NAME }),
            );
          });
        });

        describe('AND WHEN the endpoint group is modified with an invalid value', () => {
          beforeEach(async () => {
            await componentHarness.writeToEndpointGroupNameInput(INVALID_ENDPOINT_GROUP_NAME);
          });

          it('THEN the save button should be invalid', async () => {
            expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeTruthy();
          });
        });

        describe('AND WHEN the changes made have been dismissed', () => {
          beforeEach(async () => {
            await componentHarness.writeToEndpointGroupNameInput('TEST');

            await componentHarness.clickEndpointGroupDismissButton();
          });

          it('THEN the endpoint group name should reset back to the original value', async () => {
            expect(await componentHarness.readEndpointGroupNameInput()).toEqual(DEFAULT_GROUP_NAME);
          });
        });
      });

      // General Tab -> Endpoint Group Load Balancer Type
      it('THEN the endpoint group load balancer field should be visible with a default value', async () => {
        expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual(DEFAULT_LOAD_BALANCER_TYPE);
      });

      describe('AND WHEN the endpoint group load balancer type is modified', () => {
        beforeEach(async () => {
          await componentHarness.writeToEndpointGroupLoadBalancerSelector(ALTERNATE_LOAD_BALANCER_TYPE);
        });

        it('THEN the endpoint group name field should be updated with the expected value', async () => {
          expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual(ALTERNATE_LOAD_BALANCER_TYPE);
        });

        describe('AND WHEN the save button is clicked', () => {
          it('THEN the endpoint group load balancer type field should be saved', async () => {
            await componentHarness.clickEndpointGroupSaveButton();
            expectApiGetRequest(api, fixture, httpTestingController);
            expect(expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups).toEqual(
              getExpectedEndpoints(api, {
                name: DEFAULT_GROUP_NAME,
                loadBalancer: { type: ALTERNATE_LOAD_BALANCER_TYPE },
              }),
            );
          });
        });

        describe('AND WHEN the changes made have been dismissed', () => {
          beforeEach(async () => {
            await componentHarness.writeToEndpointGroupLoadBalancerSelector(ALTERNATE_LOAD_BALANCER_TYPE_2);

            await componentHarness.clickEndpointGroupDismissButton();
          });

          it('THEN the endpoint group load balancer type should reset back to the original value', async () => {
            expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual(DEFAULT_LOAD_BALANCER_TYPE);
          });
        });
      });
      it('THEN should show the configuration tab', async () => {
        expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
      });
    });

    // Back Button
    describe('WHEN the back button is clicked', () => {
      const routerSpy = jest.spyOn(FAKE_UI_ROUTER, 'go');

      beforeEach(async () => {
        await componentHarness.clickBackButton();
        expectApiSchemaGetRequests(api, fixture, httpTestingController);
      });

      it('THEN the endpoint groups list page should be the next page expected to be shown', async () => {
        expect(routerSpy).toHaveBeenCalledWith('management.apis.endpoint-groups', undefined, undefined);
      });
    });
  });

  describe('GIVEN the current page is a mock endpoint group page', () => {
    beforeEach(async () => {
      const apiWithMockEndpointGroup = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4({ type: 'mock' })] });
      await initComponent(apiWithMockEndpointGroup);
    });

    it('THEN the configuration tab is not visible', async () => {
      expect(await componentHarness.configurationTabIsVisible()).toEqual(false);
    });
  });
});
