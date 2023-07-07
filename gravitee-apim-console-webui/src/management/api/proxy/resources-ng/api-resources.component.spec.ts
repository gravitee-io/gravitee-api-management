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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject } from 'rxjs';

import { ApiResourcesComponent } from './api-resources.component';
import { ApiResourcesModule } from './api-resources.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { User } from '../../../../entities/user';
import { fakeResourceListItem } from '../../../../entities/resource/resourceListItem.fixture';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';

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

  const dialog = {
    open: jest.fn().mockReturnValue({
      afterClosed: jest.fn().mockReturnValue(new Subject()),
    }),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiResourcesModule, GioUiRouterTestingModule, MatDialogModule],
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

        {
          provide: MatDialog,
          useValue: dialog,
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

  const createComponent = (api: ApiV4) => {
    expectGetApi(api);
    fixture.detectChanges();
  };

  it('should setup properties and save changes', async () => {
    const api = fakeApiV4({ id: API_ID });
    createComponent(api);

    expect(component.api.resources).toEqual(api.resources);

    component.onChange({
      detail: {
        resources: [
          {
            name: 'my-update-cache',
            type: 'cache',
            enabled: true,
            configuration: {
              timeToIdleSeconds: 20,
              timeToLiveSeconds: 40,
              maxEntriesLocalHeap: 60,
            },
          },
        ],
      },
    });

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();
    fixture.detectChanges();

    expectGetApi(api);

    const updateRequest = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });

    const expectedResources = [
      {
        name: 'my-update-cache',
        type: 'cache',
        enabled: true,
        configuration: {
          timeToIdleSeconds: 20,
          timeToLiveSeconds: 40,
          maxEntriesLocalHeap: 60,
        },
      },
    ];
    expect(updateRequest.request.body.resources).toEqual(expectedResources);

    updateRequest.flush(api);
  });

  it('should open dialog calling on display-resource-cta event', async () => {
    createComponent(fakeApiV4({ id: API_ID }));

    component.resourceTypes = [
      {
        name: 'resource',
        description: 'resource description',
        schema: '{}',
        version: '0',
        icon: 'resource-icon',
        id: 'resource-id',
        feature: 'apim-en-schema-registry-provider',
      },
    ];

    component.onDisplayResourceCTA(
      new CustomEvent('display-resource-cta', {
        detail: {
          detail: {
            id: 'resource-id',
          },
        },
      }),
    );

    expect(dialog.open).toHaveBeenCalledWith(expect.any(Function), {
      data: {
        featureInfo: {
          description:
            'Confluent Schema Registry is part of Gravitee Enterprise. Integration with a Schema Registry enables your APIs to validate schemas used in API calls, and serialize and deserialize data.',
          image: 'assets/gio-ee-unlock-dialog/confluent-schema-registry.svg',
        },
        trialURL:
          'https://gravitee.io/self-hosted-trial?utm_source=oss_apim&utm_medium=resource_confluent_schema_registry&utm_campaign=oss_apim_to_ee_apim',
      },
      id: 'dialog',
      role: 'alertdialog',
    });
  });

  it('should disable field when origin is kubernetes', async () => {
    const api = fakeApiV4({
      id: API_ID,
      definitionContext: { origin: 'KUBERNETES' },
    });
    createComponent(api);
    expect(component.isReadonly).toEqual(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  const expectGetApi = (api: ApiV4) => {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  };
});
