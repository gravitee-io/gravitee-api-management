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

import {CONSTANTS_TESTING, GioHttpTestingModule} from '../../../../../shared/testing';
import {CurrentUserService, UIRouterState, UIRouterStateParams} from '../../../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../../../entities/user';
import {ApiV4, fakeApiV4} from "../../../../../entities/management-api-v2";
import {MatButtonHarness} from "@angular/material/button/testing";
import {HarnessLoader} from "@angular/cdk/testing";
import {ApiProxyGroupEditComponent} from "../../../proxy/endpoints/groups/edit/api-proxy-group-edit.component";
import {ApiProxyGroupsModule} from "../../../proxy/endpoints/groups/api-proxy-groups.module";
import {fakeConnectorListItem} from "../../../../../entities/connector/connector-list-item.fixture";
import {fakeResourceListItem} from "../../../../../entities/resource/resourceListItem.fixture";
import {By} from "@angular/platform-browser";
import {fakeApi} from "../../../../../entities/api/Api.fixture";
import {Api} from "../../../../../entities/api";
import {MatTabHarness} from "@angular/material/tabs/testing";
import {MatInputHarness} from "@angular/material/input/testing";

describe('ApiEndpointsGroupsComponent', () => {
  const fakeUiRouter = { go: jest.fn() };
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const testInputValue = 'New Endpoint Group Name';
  const invalidInputValue =  '';

  let fixture: ComponentFixture<ApiEndpointGroupComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiEndpointGroupHarness;
  let loader: HarnessLoader;

  const initComponent = async () => {
    const currentUser = new DeprecatedUser();

    currentUser.userPermissions = ['api-definition-u', 'api-definition-c', 'api-definition-r'];

    const routerParams: unknown = { apiId: API_ID, groupIndex: 0 };

    const apiV4 = fakeApiV4({
      id: API_ID,
    });

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEndpointGroupModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: routerParams },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    fixture = TestBed.createComponent(ApiEndpointGroupComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupHarness);
    expectApiGetRequest(apiV4);

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
    beforeEach(async () => {
      await TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiEndpointGroupComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    describe('WHEN the general tab is selected', function () {
      let api: ApiV4;

      beforeEach(async () => {
        api = fakeApiV4({
          id: API_ID,
          endpointGroups: [
            {
              name: DEFAULT_GROUP_NAME,
              type: 'kafka',

            }
          ],
        });
        expectApiGetRequest(api);

        await componentHarness.clickGeneralTab();
        fixture.detectChanges();
      });

      it('THEN the endpoint group name field should be visible with a default value', async function () {
        expect(await componentHarness.readEndpointGroupNameInput()).toEqual(DEFAULT_GROUP_NAME);
      });
    });

    describe('WHEN the endpoint group name is modified', function () {
      let api: ApiV4;

      beforeEach(async () => {
        api = fakeApiV4({
          id: API_ID,
          endpointGroups: [
            {
              name: DEFAULT_GROUP_NAME,
              type: 'kafka',

            }
          ],
        });
        expectApiGetRequest(api);

        await componentHarness.clickGeneralTab();
        fixture.detectChanges();
      });

      it('THEN the endpoint group name field should be updated with the new value', async function () {
        await componentHarness.writeToEndpointGroupNameInput(testInputValue);

        expect(await componentHarness.readEndpointGroupNameInput()).toEqual(testInputValue);
      });

      describe('AND the endpoint group name is invalid', function ()  {
        beforeEach(async () => {
          await componentHarness.writeToEndpointGroupNameInput(invalidInputValue);
          fixture.detectChanges();
        });

        it('THEN the save button should be disabled', async function () {
          expect(await componentHarness.isGeneralTabSaveButtonInvalid()).toEqual(true);
        });
      });

    });

    xdescribe('WHEN the general tab save button is clicked', async () => {
      xit('THEN the  ', async () => {

      });
    });



  describe('WHEN the back button is clicked', () => {
    it('THEN the endpoint groups list page should be the next page expected to be shown ', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');
      const apiV4 = fakeApiV4({
        id: API_ID,
      });

      await componentHarness.clickBackButton();

      expectApiGetRequest(apiV4);

      expect(routerSpy).toHaveBeenCalledWith('management.apis.ng.endpoints', undefined, undefined);
    });
  });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
