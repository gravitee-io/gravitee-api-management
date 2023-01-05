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
import { GioFormTagsInputHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';

import { ApiPortalPlanEditComponent } from './api-portal-plan-edit.component';
import { ApiPortalPlanEditModule } from './api-portal-plan-edit.module';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { Tag } from '../../../../../entities/tag/tag';
import { Group } from '../../../../../entities/group/group';
import { fakeGroup } from '../../../../../entities/group/group.fixture';
import { fakeTag } from '../../../../../entities/tag/tag.fixture';
import { Page } from '../../../../../entities/page';
import { fakeApi } from '../../../../../entities/api/Api.fixture';
import { Api } from '../../../../../entities/api';
import { Plan } from '../../../../../entities/plan';
import { fakePlan } from '../../../../../entities/plan/plan.fixture';

describe('ApiPortalPlanEditComponent', () => {
  const API_ID = 'my-api';
  const currentUser = new User();
  const fakeUiRouter = { go: jest.fn() };
  currentUser.userPermissions = ['api-plan-u'];

  let fixture: ComponentFixture<ApiPortalPlanEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const configureTestingModule = (planId?: string) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPortalPlanEditModule, MatIconTestingModule],
      providers: [
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, planId } },
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
      ],
    });

    fixture = TestBed.createComponent(ApiPortalPlanEditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('New mode', () => {
    beforeEach(() => {
      configureTestingModule();
    });

    it('should add new plan', async () => {
      const TAG_1_ID = 'tag-1';

      expectApiGetRequest(fakeApi({ id: API_ID, tags: [TAG_1_ID] }));
      expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectDocumentationSearchRequest(API_ID, [{ id: 'doc-1', name: 'Doc 1' }]);
      expectCurrentUserTagsRequest([TAG_1_ID]);
      expectResourceGetRequest();
      fixture.detectChanges();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(true);

      // 1- General Step
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      await nameInput.setValue('🗺');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      await descriptionInput.setValue('Description');

      const characteristicsInput = await loader.getHarness(
        GioFormTagsInputHarness.with({ selector: '[formControlName="characteristics"]' }),
      );
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="generalConditions"]' }));
      await generalConditionsInput.clickOptions({ text: 'Doc 1' });

      const validationToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="validation"]' }));
      await validationToggle.toggle();

      const commentRequired = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="commentRequired"]' }));
      await commentRequired.toggle();

      const commentMessageInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="commentMessage"]' }));
      await commentMessageInput.setValue('Comment message');

      const shardingTagsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="shardingTags"]' }));
      await shardingTagsInput.clickOptions({ text: /Tag 1/ });
      await shardingTagsInput.getOptions({ text: /Tag 2/ }).then((options) => options[0].isDisabled());

      const excludedGroupsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="excludedGroups"]' }));
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      const securityTypesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="securityTypes"]' }));
      await securityTypesInput.clickOptions({ text: /JWT/ });
      await securityTypesInput.getOptions({ text: /OAuth2/ }).then((options) => expect(options.length).toBe(0));
      expectPolicySchemaGetRequest('jwt', {});

      const selectionRuleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
      await selectionRuleInput.setValue('{ #el ...}');

      // 3- Restriction Step

      const rateLimitEnabledInput = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="rateLimitEnabled"]' }),
      );
      await rateLimitEnabledInput.toggle();
      expectPolicySchemaGetRequest('rate-limit', {});

      const quotaEnabledInput = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="quotaEnabled"]' }));
      await quotaEnabledInput.toggle();
      expectPolicySchemaGetRequest('quota', {});

      const resourceFilteringEnabledInput = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="resourceFilteringEnabled"]' }),
      );
      await resourceFilteringEnabledInput.toggle();
      expectPolicySchemaGetRequest('resource-filtering', {});

      // Save
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans`, method: 'POST' });

      expect(req.request.body).toEqual({
        name: '🗺',
        description: 'Description',
        api: 'my-api',
        characteristics: ['C1'],
        comment_message: 'Comment message',
        comment_required: true,
        excluded_groups: ['group-a'],
        general_conditions: 'doc-1',
        security: 'JWT',
        securityDefinition: '{}',
        selection_rule: '{ #el ...}',
        tags: [TAG_1_ID],
        validation: 'AUTO',
        flows: [
          {
            enabled: true,
            'path-operator': {
              operator: 'STARTS_WITH',
              path: '/',
            },
            post: [],
            pre: [
              {
                configuration: {},
                enabled: true,
                name: 'Rate Limiting',
                policy: 'rate-limit',
              },
              {
                configuration: {},
                enabled: true,
                name: 'Quota',
                policy: 'quota',
              },
              {
                configuration: {},
                enabled: true,
                name: 'Resource Filtering',
                policy: 'resource-filtering',
              },
            ],
          },
        ],
      });
      req.flush({});
      expect(fakeUiRouter.go).toHaveBeenCalled();
    });
  });

  describe('Edit mode', () => {
    const PLAN_ID = 'plan-1';

    beforeEach(() => {
      configureTestingModule(PLAN_ID);
    });

    it('should edit plan', async () => {
      const TAG_1_ID = 'tag-1';
      const planToUpdate = fakePlan({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });

      expectApiGetRequest(fakeApi({ id: API_ID, tags: [TAG_1_ID] }));
      expectPlanGetRequest(API_ID, planToUpdate);

      expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectDocumentationSearchRequest(API_ID, [{ id: 'doc-1', name: 'Doc 1' }]);
      expectCurrentUserTagsRequest([TAG_1_ID]);
      expectResourceGetRequest();
      fixture.detectChanges();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      // 1- General Step
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.getValue()).toBe('Old 🗺');
      await nameInput.setValue('🗺');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      expect(await descriptionInput.getValue()).toBe('Old Description');
      await descriptionInput.setValue('Description');

      const characteristicsInput = await loader.getHarness(
        GioFormTagsInputHarness.with({ selector: '[formControlName="characteristics"]' }),
      );
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="generalConditions"]' }));
      await generalConditionsInput.clickOptions({ text: 'Doc 1' });

      const shardingTagsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="shardingTags"]' }));
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      await shardingTagsInput.clickOptions({ text: /Tag 1/ }); // Unselect Tag 1
      await shardingTagsInput.getOptions({ text: /Tag 2/ }).then((options) => options[0].isDisabled());

      const excludedGroupsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="excludedGroups"]' }));
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      const securityTypesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="securityTypes"]' }));
      expect(await securityTypesInput.isDisabled()).toEqual(true);
      expect(await securityTypesInput.getValueText()).toEqual('Keyless (public)');

      const selectionRuleInput = await loader.getAllHarnesses(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
      expect(selectionRuleInput.length).toEqual(0); // no selection rule for keyless

      // Save
      await saveBar.clickSubmit();

      expectPlanGetRequest(API_ID, fakePlan(planToUpdate));
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/plans/${PLAN_ID}`,
        method: 'PUT',
      });

      expect(req.request.body).toEqual({
        ...planToUpdate,
        name: '🗺',
        description: 'Description',
        api: 'my-api',
        characteristics: ['C1'],
        excluded_groups: ['group-a'],
        general_conditions: 'doc-1',
        security: 'KEY_LESS',
        securityDefinition: '{}',
        selection_rule: undefined,
        tags: [],
        validation: 'MANUAL',
        comment_message: undefined,
        comment_required: false,
      });
      req.flush({});
      expect(fakeUiRouter.go).toHaveBeenCalled();
    });
  });

  describe('Kubernetes mode', () => {
    const PLAN_ID = 'plan-1';

    beforeEach(() => {
      configureTestingModule(PLAN_ID);
    });

    it('should access plan in read only ', async () => {
      const TAG_1_ID = 'tag-1';
      const planToUpdate = fakePlan({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });

      expectApiGetRequest(fakeApi({ id: API_ID, tags: [TAG_1_ID], definition_context: { origin: 'kubernetes' } }));
      expectPlanGetRequest(API_ID, planToUpdate);

      expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectDocumentationSearchRequest(API_ID, [{ id: 'doc-1', name: 'Doc 1' }]);
      expectCurrentUserTagsRequest([TAG_1_ID]);
      expectResourceGetRequest();
      fixture.detectChanges();

      const saveBar = await loader.getAllHarnesses(GioSaveBarHarness);
      expect(saveBar.length).toEqual(0);

      // 1- General Step
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.getValue()).toBe('Old 🗺');
      expect(await nameInput.isDisabled()).toBe(true);

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="description"]' }));
      expect(await descriptionInput.getValue()).toBe('Old Description');
      expect(await descriptionInput.isDisabled()).toBe(true);

      const characteristicsInput = await loader.getHarness(
        GioFormTagsInputHarness.with({ selector: '[formControlName="characteristics"]' }),
      );
      expect(await characteristicsInput.isDisabled()).toBe(true);

      const generalConditionsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="generalConditions"]' }));
      expect(await generalConditionsInput.isDisabled()).toBe(true);

      const shardingTagsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="shardingTags"]' }));
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      expect(await shardingTagsInput.isDisabled()).toBe(true);

      const excludedGroupsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="excludedGroups"]' }));
      expect(await excludedGroupsInput.isDisabled()).toBe(true);

      // 2- Secure Step
      const securityTypesInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="securityTypes"]' }));
      expect(await securityTypesInput.isDisabled()).toEqual(true);

      const selectionRuleInput = await loader.getAllHarnesses(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
      expect(selectionRuleInput.length).toEqual(0); // no selection rule for keyless
    });
  });

  function expectTagsListRequest(tags: Tag[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
      })
      .flush(tags);
  }

  function expectGroupLisRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups`,
      })
      .flush(groups);
  }

  function expectDocumentationSearchRequest(apiId: string, groups: Page[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/pages?type=MARKDOWN&api=${apiId}`,
      })
      .flush(groups);
  }

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectCurrentUserTagsRequest(tags: string[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/user/tags`,
      })
      .flush(tags);
  }

  function expectResourceGetRequest() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/resources?expand=icon`, method: 'GET' }).flush([]);
  }

  function expectPolicySchemaGetRequest(type: string, schema: unknown) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/policies/${type}/schema`, method: 'GET' }).flush(schema);
  }

  function expectPlanGetRequest(apiId: string, plan: Plan) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/plans/${plan.id}`, method: 'GET' }).flush(plan);
  }
});
