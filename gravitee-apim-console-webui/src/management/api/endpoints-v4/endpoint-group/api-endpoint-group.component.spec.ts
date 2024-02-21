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
import { User as DeprecatedUser } from '../../../../entities/user';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { fakeEndpointGroupV4 } from '../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';

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
  httpTestingController.match({
    url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${api.endpointGroups[0].type}/shared-configuration-schema`,
    method: 'GET',
  });
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
  const API_ID = 'apiId';
  const FAKE_UI_ROUTER = { go: jest.fn() };
  let fixture: ComponentFixture<ApiEndpointGroupComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiEndpointGroupHarness;
  let api: ApiV4;

  const initComponent = async (api: ApiV4, routerParams: unknown = { apiId: API_ID, groupIndex: 0 }) => {
    const currentUser = new DeprecatedUser();

    currentUser.userPermissions = ['api-definition-u', 'api-definition-c', 'api-definition-r'];

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
    fixture.componentInstance.api = api;
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupHarness);

    expectApiGetRequest(api, fixture, httpTestingController);

    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('endpoint group update - configuration', () => {
    it('should not update endpoint group with invalid name', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');

      // empty string is not a valid name
      await componentHarness.writeToEndpointGroupNameInput('');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeTruthy();

      // reset change will restore initial value
      await componentHarness.clickEndpointGroupDismissButton();
      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });

    it('should update endpoint group with valid name', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupNameInput()).toEqual('default-group');

      // change to a valid name
      await componentHarness.writeToEndpointGroupNameInput('default-group updated');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // save the correct value
      await componentHarness.clickEndpointGroupSaveButton();
      expectApiGetRequest(api, fixture, httpTestingController);
      const endpointsGroup = expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups;
      expect(endpointsGroup[0].name).toEqual('default-group updated');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });

    it('should update endpoint group load balancing', async () => {
      api = fakeApiV4({ id: API_ID, endpointGroups: [fakeEndpointGroupV4()] });
      await initComponent(api);
      await componentHarness.clickGeneralTab();
      expectApiSchemaGetRequests(api, fixture, httpTestingController);

      expect(await componentHarness.readEndpointGroupLoadBalancerSelector()).toEqual('ROUND_ROBIN');

      // change load balancing strategy
      await componentHarness.writeToEndpointGroupLoadBalancerSelector('RANDOM');
      expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toBeFalsy();

      // save the new strategy
      await componentHarness.clickEndpointGroupSaveButton();
      expectApiGetRequest(api, fixture, httpTestingController);
      const endpointsGroup = expectApiPutRequest(api, fixture, httpTestingController).request.body.endpointGroups;
      expect(endpointsGroup[0].name).toEqual('default-group');
      expect(endpointsGroup[0].loadBalancer.type).toEqual('RANDOM');
      expect(await componentHarness.configurationTabIsVisible()).toEqual(true);
    });
  });
});
