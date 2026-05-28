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
import { ComponentFixture, fakeAsync, flush, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';
import { MatStepperHarness } from '@angular/material/stepper/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { ApiPlanEditComponent } from './api-plan-edit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiPlansModule } from '../api-plans.module';
import { fakeTag } from '../../../../entities/tag/tag.fixture';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { ApiPlanFormHarness } from '../../component/plan/api-plan-form.harness';
import {
  Api,
  CreatePlanV2,
  CreatePlanV4,
  fakeApiFederated,
  fakeApiV2,
  fakeApiV4,
  fakeNativeKafkaApiV4,
  fakePlanFederated,
  fakePlanV2,
  fakePlanV4,
  Plan,
  PlanStatus,
  PlanV2,
} from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';

describe('ApiPlanEditComponent', () => {
  const API_ID = 'my-api';

  let fixture: ComponentFixture<ApiPlanEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigationSpy: jest.SpyInstance;

  const configureTestingModule = (planId: string = undefined) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiPlansModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: ['api-plan-u'] },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { apiId: API_ID, planId }, queryParams: { selectedPlanMenuItem: 'JWT' } },
          },
        },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              oauth2: { enabled: false },
              jwt: { enabled: true },
            });
            return constants;
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ApiPlanEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigationSpy = jest.spyOn(router, 'navigate');
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
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('jwt', {});

        await planForm.getNameInput().then(i => i.setValue('My new plan'));

        // Advance stepper by selecting steps so Submit shows (avoids interval(100) timeout)
        const stepper = await loader.getHarness(MatStepperHarness);
        const steps = await stepper.getSteps();
        await steps[1].select();
        await steps[2].select();
        fixture.detectChanges();

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
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], {
          queryParams: {
            status: 'STAGING',
          },
          relativeTo: expect.anything(),
        });
      });
    });

    describe('Edit', () => {
      const TAG_1_ID = 'tag-1';
      const PLAN = fakePlanV2({ apiId: API_ID, security: { type: 'KEY_LESS' } });

      beforeEach(async () => {
        configureTestingModule(PLAN.id);
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV2({ id: API_ID }));
      });

      it.each(['STAGING', 'PUBLISHED', 'DEPRECATED'])(
        'should edit plan',
        fakeAsync(async (status: PlanStatus) => {
          PLAN.status = status;
          expectPlanGetRequest(API_ID, PLAN);

          const saveBar = await loader.getHarness(GioSaveBarHarness);
          expect(await saveBar.isVisible()).toBe(false);

          const planForm = await loader.getHarness(ApiPlanFormHarness);

          planForm
            .httpRequest(httpTestingController)
            .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
          planForm.httpRequest(httpTestingController).expectGroupListRequest([
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
            status: PLAN.status,
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
          expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], {
            queryParams: {
              status: PLAN.status,
            },
            relativeTo: expect.anything(),
          });
          flush();
        }),
      );
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
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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

  describe('With a V4 API', () => {
    describe('Edit', () => {
      const TAG_1_ID = 'tag-1';
      const PLAN = fakePlanV4({ apiId: API_ID, mode: 'PUSH' });

      beforeEach(async () => {
        configureTestingModule(PLAN.id);
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV4({ id: API_ID }));
        expectPlanGetRequest(API_ID, PLAN);
      });

      it('should edit plan', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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

        expect(req.request.body).toEqual(expect.objectContaining({ name: 'My plan edited' }));
        req.flush({});
        expect(routerNavigationSpy).toHaveBeenCalled();
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
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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

  describe('With a V4 Native API', () => {
    describe('Create', () => {
      const TAG_1_ID = 'tag-1';

      beforeEach(() => {
        configureTestingModule();
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV4({ id: API_ID, type: 'NATIVE' }));
      });

      it('should create new plan', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(true);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('jwt', {});

        await planForm.getNameInput().then(i => i.setValue('My new plan'));

        // Advance stepper by selecting next step so Submit shows
        const stepper = await loader.getHarness(MatStepperHarness);
        const steps = await stepper.getSteps();
        await steps[1].select();
        fixture.detectChanges();

        await saveBar.clickSubmit();

        const req = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans`,
          method: 'POST',
        });

        expect(req.request.body).toEqual({
          definitionVersion: 'V4',
          name: 'My new plan',
          description: '',
          commentMessage: '',
          commentRequired: false,
          mode: 'STANDARD',
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
          flows: [],
        } as CreatePlanV4);
        req.flush({});
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], {
          queryParams: {
            status: 'STAGING',
          },
          relativeTo: expect.anything(),
        });
      });
    });

    describe('Edit', () => {
      const TAG_1_ID = 'tag-1';
      const PLAN = fakePlanV4({ apiId: API_ID, security: { type: 'KEY_LESS' } });

      beforeEach(() => {
        configureTestingModule(PLAN.id);
        fixture.detectChanges();
        expectApiGetRequest(fakeApiV4({ id: API_ID, type: 'NATIVE' }));
        expectPlanGetRequest(API_ID, PLAN);
      });

      it('should edit plan', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([
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

        expect(req.request.body).toEqual(expect.objectContaining({ name: 'My plan edited' }));
        req.flush({});
        expect(routerNavigationSpy).toHaveBeenCalled();
      });
    });
  });

  describe('With a deployed Native Kafka API — bootstrap port confirmation dialog', () => {
    const TAG_1_ID = 'tag-1';
    const PLAN_WITH_BOOTSTRAP = fakePlanV4({ apiId: API_ID, security: { type: 'KEY_LESS' }, bootstrapPort: 9092 });
    const DEPLOYED_NATIVE_API = fakeNativeKafkaApiV4({ id: API_ID, deployedAt: new Date() });

    const setupDeployedNativeEdit = () => {
      configureTestingModule(PLAN_WITH_BOOTSTRAP.id);
      fixture.detectChanges();
      expectApiGetRequest(DEPLOYED_NATIVE_API);
      expectPlanGetRequest(API_ID, PLAN_WITH_BOOTSTRAP);
    };

    const flushPlanFormRequests = (planForm: any) => {
      planForm.httpRequest(httpTestingController).expectTagsListRequest([{ id: TAG_1_ID, name: 'Tag 1', key: TAG_1_ID }]);
      planForm.httpRequest(httpTestingController).expectGroupListRequest([]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API_ID, []);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([]);
    };

    it('should open the dialog when bootstrap port changes, and abort save when dialog resolves false', async () => {
      setupDeployedNativeEdit();

      const planForm = await loader.getHarness(ApiPlanFormHarness);
      flushPlanFormRequests(planForm);
      fixture.detectChanges();

      // Change the name to dirty the form
      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('Updated plan');

      // Change the bootstrap port to a new value
      const bootstrapPortInput = await planForm.getBootstrapPortInput();
      await bootstrapPortInput.setValue('9999');
      fixture.detectChanges();

      const matDialog = TestBed.inject(MatDialog);
      const dialogSpy = jest.spyOn(matDialog, 'open').mockReturnValue({
        afterClosed: () => of(false),
      } as any);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expect(dialogSpy).toHaveBeenCalled();
      httpTestingController.expectNone({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_WITH_BOOTSTRAP.id}`,
        method: 'PUT',
      });
      expect(routerNavigationSpy).not.toHaveBeenCalled();
    });

    it('should save when dialog resolves true after bootstrap port change', async () => {
      setupDeployedNativeEdit();

      const planForm = await loader.getHarness(ApiPlanFormHarness);
      flushPlanFormRequests(planForm);
      fixture.detectChanges();

      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('Updated plan');

      const bootstrapPortInput = await planForm.getBootstrapPortInput();
      await bootstrapPortInput.setValue('9999');
      fixture.detectChanges();

      const matDialog = TestBed.inject(MatDialog);
      jest.spyOn(matDialog, 'open').mockReturnValue({
        afterClosed: () => of(true),
      } as any);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expectPlanGetRequest(API_ID, PLAN_WITH_BOOTSTRAP);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_WITH_BOOTSTRAP.id}`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual(expect.objectContaining({ name: 'Updated plan' }));
      req.flush({});
      expect(routerNavigationSpy).toHaveBeenCalled();
    });

    it('should save directly (no dialog) when bootstrap port is unchanged', async () => {
      setupDeployedNativeEdit();

      const planForm = await loader.getHarness(ApiPlanFormHarness);
      flushPlanFormRequests(planForm);
      fixture.detectChanges();

      // Change only the name, not the bootstrap port
      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('Updated plan');
      fixture.detectChanges();

      const matDialog = TestBed.inject(MatDialog);
      const dialogSpy = jest.spyOn(matDialog, 'open');

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      expect(dialogSpy).not.toHaveBeenCalled();

      expectPlanGetRequest(API_ID, PLAN_WITH_BOOTSTRAP);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_WITH_BOOTSTRAP.id}`,
        method: 'PUT',
      });
      req.flush({});
      expect(routerNavigationSpy).toHaveBeenCalled();
    });
  });

  describe('With a Federated API', () => {
    const PLAN = fakePlanFederated({ apiId: API_ID });

    beforeEach(async () => {
      configureTestingModule(PLAN.id);
      fixture.detectChanges();
      expectApiGetRequest(fakeApiFederated({ id: API_ID }));
      expectPlanGetRequest(API_ID, PLAN);
    });

    it('should only display allowed attributes', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const planForm = await loader.getHarness(ApiPlanFormHarness);
      planForm.httpRequest(httpTestingController).expectGroupListRequest([
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
      expect(await planForm.getShardingTagsInput()).toBeNull();

      const stepper = await loader.getHarness(MatStepperHarness);
      const stepLabels = await stepper.getSteps().then(steps => Promise.all(steps.map(s => s.getLabel())));
      expect(stepLabels).toEqual(['General']);
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
