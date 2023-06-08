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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { UIRouterGlobals } from '@uirouter/core';

import { ApiPortalPlanEditComponent } from './api-portal-plan-edit.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiPortalPlansModule } from '../api-portal-plans.module';
import { fakeTag } from '../../../../../entities/tag/tag.fixture';
import { fakeGroup } from '../../../../../entities/group/group.fixture';
import { ApiPlanFormHarness } from '../../../component/plan/api-plan-form.harness';
import { Api, CreatePlanV2, fakeApiV2, fakePlanV2, Plan, PlanV2 } from '../../../../../entities/management-api-v2';

describe('ApiPortalPlanEditComponent', () => {
  const API_ID = 'my-api';
  const currentUser = new User();
  const fakeUiRouter = { go: jest.fn() };
  const fakeAjsGlobals = { current: { data: { baseRouteState: 'management.apis.ng' } } };

  currentUser.userPermissions = ['api-plan-u'];

  let fixture: ComponentFixture<ApiPortalPlanEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const configureTestingModule = (planId: string = undefined, ajsGlobals: any = {}) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalPlansModule, MatIconTestingModule],
      providers: [
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, planId, securityType: 'JWT' } },
        { provide: UIRouterState, useValue: fakeUiRouter },
        {
          provide: 'Constants',
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              oauth2: { enabled: false },
              jwt: { enabled: true },
            });
            return constants;
          },
        },
        { provide: UIRouterGlobals, useValue: ajsGlobals },
      ],
    });

    fixture = TestBed.createComponent(ApiPortalPlanEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('With a V2 API', () => {
    describe('Create', () => {
      const TAG_1_ID = 'tag-1';

      beforeEach(() => {
        configureTestingModule();
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV2({ id: API_ID }));
      });

      it('should create new plan', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(true);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupLisRequest([
          fakeGroup({
            id: 'group-a',
            name: 'Group A',
          }),
        ]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API_ID, [
          {
            id: 'doc-1',
            name: 'Doc 1',
          },
        ]);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
        planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('jwt', {});

        await planForm.getNameInput().then((i) => i.setValue('My new plan'));

        // Click on Next buttons to display Save one
        await loader.getHarness(MatButtonHarness.with({ text: 'Next' })).then((b) => b.click());
        await loader.getHarness(MatButtonHarness.with({ text: 'Next' })).then((b) => b.click());

        await saveBar.clickSubmit();

        const req = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`,
          method: 'POST',
        });

        expect(req.request.body).toEqual({
          definitionVersion: 'V2',
          name: 'My new plan',
          description: '',
          commentMessage: '',
          commentRequired: false,
          validation: 'MANUAL',
          generalConditions: '',
          characteristics: [],
          excludedGroups: [],
          tags: [],
          security: {
            type: 'JWT',
            configuration: {},
          },
          selectionRule: null,
          flows: [
            {
              enabled: true,
              pathOperator: {
                operator: 'STARTS_WITH',
                path: '/',
              },
              post: [],
              pre: [],
            },
          ],
        } as CreatePlanV2);
        req.flush({});
        expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.detail.portal.plans');
      });
    });

    describe('Edit', () => {
      const TAG_1_ID = 'tag-1';
      const PLAN = fakePlanV2({ apiId: API_ID, security: { type: 'KEY_LESS' } });

      beforeEach(async () => {
        configureTestingModule(PLAN.id);
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV2({ id: API_ID }));
        expectPlanGetRequest(API_ID, PLAN);
      });

      it('should edit plan', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupLisRequest([
          fakeGroup({
            id: 'group-a',
            name: 'Group A',
          }),
        ]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API_ID, [
          {
            id: 'doc-1',
            name: 'Doc 1',
          },
        ]);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);

        const nameInput = await planForm.getNameInput();
        await nameInput.setValue('My plan edited');

        expect(await saveBar.isVisible()).toBe(true);
        await saveBar.clickSubmit();

        expectPlanGetRequest(API_ID, PLAN);
        const req = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN.id}`,
          method: 'PUT',
        });

        expect(req.request.body).toEqual({
          apiId: API_ID,
          name: 'My plan edited',
          description: 'Default plan',
          characteristics: [],
          excludedGroups: undefined,
          status: 'PUBLISHED',
          validation: 'MANUAL',
          tags: ['tag1'],
          commentMessage: 'comment message',
          commentRequired: false,
          generalConditions: 'general conditions',
          id: PLAN.id,
          security: {
            type: 'KEY_LESS',
            configuration: undefined,
          },
          selectionRule: undefined,
          definitionVersion: 'V2',
          flows: [
            {
              name: '',
              pathOperator: {
                path: '/',
                operator: 'STARTS_WITH',
              },
              condition: '',
              consumers: [],
              methods: [],
              pre: [
                {
                  name: 'Mock',
                  description: 'Saying hello to the world',
                  enabled: true,
                  policy: 'mock',
                  configuration: { content: 'Hello world', status: '200' },
                },
              ],
              post: [],
              enabled: true,
            },
          ],
        } as PlanV2);
        req.flush({});
        expect(fakeUiRouter.go).toHaveBeenCalled();
      });
    });

    describe('Edit Kubernetes API', () => {
      const TAG_1_ID = 'tag-1';
      const PLAN = fakePlanV2({ apiId: API_ID, security: { type: 'KEY_LESS' } });

      beforeEach(async () => {
        configureTestingModule(PLAN.id);
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV2({ id: API_ID, definitionContext: { origin: 'KUBERNETES' } }));
        expectPlanGetRequest(API_ID, PLAN);
      });

      it('should access plan in read only', async () => {
        expect(await loader.getAllHarnesses(GioSaveBarHarness)).toHaveLength(0);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupLisRequest([
          fakeGroup({
            id: 'group-a',
            name: 'Group A',
          }),
        ]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API_ID, [
          {
            id: 'doc-1',
            name: 'Doc 1',
          },
        ]);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);

        const nameInput = await planForm.getNameInput();
        expect(await nameInput.isDisabled()).toEqual(true);
      });
    });
  });

  describe('with new Angular router', () => {
    const TAG_1_ID = 'tag-1';

    beforeEach(() => {
      configureTestingModule(null, fakeAjsGlobals);
      fixture.detectChanges();
      expectApiGetRequest(fakeApiV2({ id: API_ID }));
    });

    it('should create new plan', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([
        fakeGroup({
          id: 'group-a',
          name: 'Group A',
        }),
      ]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API_ID, [
        {
          id: 'doc-1',
          name: 'Doc 1',
        },
      ]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('jwt', {});

      await planForm.getNameInput().then((i) => i.setValue('My new plan'));

      // Click on Next buttons to display Save one
      await loader.getHarness(MatButtonHarness.with({ text: 'Next' })).then((b) => b.click());
      await loader.getHarness(MatButtonHarness.with({ text: 'Next' })).then((b) => b.click());

      await saveBar.clickSubmit();

      httpTestingController
        .expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`,
          method: 'POST',
        })
        .flush({});
      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.ng.plans');
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectPlanGetRequest(apiId: string, plan: Plan) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${plan.id}`, method: 'GET' })
      .flush(plan);
  }
});
