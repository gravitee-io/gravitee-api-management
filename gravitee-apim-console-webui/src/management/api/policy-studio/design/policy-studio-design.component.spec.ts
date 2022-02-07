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

import { PolicyStudioDesignComponent } from './policy-studio-design.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakePolicyListItem } from '../../../../entities/policy';
import { ManagementModule } from '../../../management.module';
import { fakeApiFlowSchema } from '../../../../entities/flow/apiFlowSchema.fixture';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { fakeResourceListItem } from '../../../../entities/resource/resourceListItem.fixture';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { fakeUpdateApi } from '../../../../entities/api/UpdateApi.fixture';

describe('PolicyStudioDesignComponent', () => {
  let fixture: ComponentFixture<PolicyStudioDesignComponent>;
  let component: PolicyStudioDesignComponent;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userApiPermissions = ['api-plan-r', 'api-plan-u'];

  const apiFlowSchema = fakeApiFlowSchema();
  const policies = [fakePolicyListItem()];
  const api = fakeApi();
  const resources = [fakeResourceListItem()];
  const ajsRootScopeBroadcastMock = jest.fn();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ManagementModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: api.id } },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
        },
        { provide: AjsRootScope, useValue: { $broadcast: ajsRootScopeBroadcastMock } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PolicyStudioDesignComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/schema`).flush(apiFlowSchema);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon`).flush(policies);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`).flush(resources);
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      expect(component.apiFlowSchema).toStrictEqual(apiFlowSchema);
      expect(component.policies).toStrictEqual(policies);
      expect(component.resourceTypes).toStrictEqual(resources);
      expect(component.services).toStrictEqual(api.services);
      expect(component.definition).toStrictEqual({
        flows: [
          {
            condition: '',
            consumers: [],
            enabled: true,
            methods: [],
            name: '',
            'path-operator': { operator: 'STARTS_WITH', path: '/' },
            post: [],
            pre: [
              {
                configuration: {
                  content: 'Hello world',
                  status: '200',
                },
                description: 'Saying hello to the world',
                enabled: true,
                name: 'Mock',
                policy: 'mock',
              },
            ],
          },
        ],
        'flow-mode': 'DEFAULT',
        name: '🪐 Planets',
        plans: [],
        properties: [],
        resources: [
          {
            configuration: {
              maxEntriesLocalHeap: 1000,
              timeToIdleSeconds: 60,
              timeToLiveSeconds: 60,
            },
            enabled: true,
            name: 'my-cache',
            type: 'cache',
          },
        ],
        version: '1.0',
      });
    });
  });

  describe('onSave', () => {
    it('should call the API', async () => {
      component.onSave({
        definition: {
          name: api.name,
          version: api.version,
          flows: [],
          resources: [],
          plans: [],
          properties: [],
          'flow-mode': 'BEST_MATCH',
        },
        services: api.services,
      });

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}` });

      expect(req.request.body).toStrictEqual(
        fakeUpdateApi({
          background: undefined,
          categories: undefined,
          paths: undefined,
          picture: undefined,
          flows: [],
          resources: [],
          plans: [],
          properties: [],
          flow_mode: 'BEST_MATCH',
        }),
      );
      req.flush(api);

      expect(ajsRootScopeBroadcastMock).toHaveBeenCalledWith('apiChangeSuccess', { api });

      // call new ngOnInit
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/schema`).flush(apiFlowSchema);
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon`).flush(policies);
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`).flush(resources);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
