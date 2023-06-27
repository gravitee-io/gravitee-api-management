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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApiResourcesComponent } from './api-resources.component';
import { ApiResourcesModule } from './api-resources.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { User } from '../../../../entities/user';
import { fakeResourceListItem } from '../../../../entities/resource/resourceListItem.fixture';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api } from '../../../../entities/api';

describe('PolicyStudioResourcesComponent', () => {
  let fixture: ComponentFixture<ApiResourcesComponent>;
  let component: ApiResourcesComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const currentUser = new User();
  currentUser.userPermissions = ['api-plan-r', 'api-plan-u'];
  const API_ID = 'apiId';
  const $broadcast = jest.fn();

  const resources = [fakeResourceListItem()];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiResourcesModule, GioUiRouterTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        {
          provide: AjsRootScope,
          useValue: {
            $broadcast: $broadcast,
          },
        },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ApiResourcesComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`).flush(resources);
  });

  const createComponent = (api: Api) => {
    expectGetApi(api);
    fixture.detectChanges();
  };

  it('should setup properties and save changes', async () => {
    const api = fakeApi({ id: API_ID });
    createComponent(api);

    expect(component.apiDefinition).toStrictEqual({
      id: api.id,
      name: api.name,
      origin: 'management',
      flows: api.flows,
      flow_mode: api.flow_mode,
      resources: api.resources,
      plans: api.plans,
      version: api.version,
      services: api.services,
      properties: api.properties,
      execution_mode: api.execution_mode,
    });

    component.onChange({
      detail: {
        resources: [
          {
            name: 'my-cache',
            type: 'cache',
            enabled: true,
            configuration: {
              timeToIdleSeconds: 0,
              timeToLiveSeconds: 0,
              maxEntriesLocalHeap: 1000,
            },
          },
        ],
      },
    });

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();
    fixture.detectChanges();

    expectGetApi(api);

    const updateRequest = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'PUT' });

    const expectedResources = [
      {
        name: 'my-cache',
        type: 'cache',
        enabled: true,
        configuration: {
          timeToIdleSeconds: 0,
          timeToLiveSeconds: 0,
          maxEntriesLocalHeap: 1000,
        },
      },
    ];
    expect(updateRequest.request.body.resources).toEqual(expectedResources);

    updateRequest.flush(api);
  });

  it('should disable field when origin is kubernetes', async () => {
    const api = fakeApi({
      id: API_ID,
      definition_context: { origin: 'kubernetes' },
    });
    createComponent(api);
    expect(component.isReadonly).toEqual(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const expectGetApi = (api: Api) => {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  };
});
