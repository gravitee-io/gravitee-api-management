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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { set } from 'lodash';
import { Component, ViewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { ApiPlanFormModule } from './api-plan-form.module';
import { ApiPlanFormHarness } from './api-plan-form.harness';
import { ApiPlanFormComponent } from './api-plan-form.component';

import { CurrentUserService } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { fakeTag } from '../../../../entities/tag/tag.fixture';
import { fakeApi as fakeApiV3 } from '../../../../entities/api/Api.fixture';
import { fakeApiEntity as fakeApiV4 } from '../../../../entities/api-v4/ApiEntity.fixture';
import { Api as ApiV3 } from '../../../../entities/api';
import { ApiEntity as ApiV4 } from '../../../../entities/api-v4';
import { Plan, PlanSecurityType } from '../../../../entities/plan';
import { fakePlan as fakePlanV3 } from '../../../../entities/plan/plan.fixture';
import { fakeV4Plan as fakePlanV4 } from '../../../../entities/plan-v4/plan.fixture';

@Component({
  template: ` <api-plan-form #apiPlanForm [formControl]="planControl" [mode]="mode" [api]="api"></api-plan-form> `,
})
class TestComponent {
  @ViewChild('apiPlanForm') apiPlanForm: ApiPlanFormComponent;
  mode: 'create' | 'edit' = 'create';
  planControl = new FormControl();
  api?: ApiV3 | ApiV4;
  plan?: Plan;
}

const fakeApiKeySchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    propagateApiKey: {
      title: 'Propagate API Key to upstream API',
      type: 'boolean',
    },
  },
  additionalProperties: false,
};

describe('ApiPlanFormComponent', () => {
  const currentUser = new User();
  currentUser.userPermissions = ['api-plan-u'];

  let fixture: ComponentFixture<TestComponent>;
  let testComponent: TestComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const configureTestingModule = (mode: 'create' | 'edit', api?: ApiV3 | ApiV4) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ReactiveFormsModule, NoopAnimationsModule, GioHttpTestingModule, ApiPlanFormModule, MatIconTestingModule],
      providers: [
        { provide: CurrentUserService, useValue: { currentUser } },
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

    fixture = TestBed.createComponent(TestComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    testComponent = fixture.componentInstance;
    testComponent.mode = mode;
    testComponent.api = api;
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create mode V3 with API', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV3({
      tags: [TAG_1_ID],
      resources: [
        {
          name: 'OAuth2 AM Resource',
          enabled: true,
          type: 'oauth2-am',
          configuration: {},
        },
      ],
    });
    beforeEach(async () => {
      configureTestingModule('create', API);
      fixture.detectChanges();
    });

    it('should add new plan', async () => {
      loader = TestbedHarnessEnvironment.loader(fixture);
      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      await descriptionInput.setValue('Description');

      const characteristicsInput = await planForm.getCharacteristicsInput();
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      await generalConditionsInput.clickOptions({ text: 'Doc 1' });

      const validationToggle = await planForm.getValidationToggle();
      await validationToggle.toggle();

      const commentRequired = await planForm.getCommentRequiredToggle();
      await commentRequired.toggle();

      const commentMessageInput = await planForm.getCommentMessageInput();
      await commentMessageInput.setValue('Comment message');

      const shardingTagsInput = await planForm.getShardingTagsInput();
      await shardingTagsInput.clickOptions({ text: /Tag 1/ });
      await shardingTagsInput.getOptions({ text: /Tag 2/ }).then((options) => options[0].isDisabled());

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      expect(testComponent.apiPlanForm.hasNextStep()).toEqual(true);
      expect(testComponent.apiPlanForm.hasPreviousStep()).toEqual(false);
      testComponent.apiPlanForm.nextStep();
      fixture.detectChanges();

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      await securityTypeInput.clickOptions({ text: /JWT/ });
      await securityTypeInput.getOptions({ text: /OAuth2/ }).then((options) => expect(options.length).toBe(0));
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('jwt', {});

      const selectionRuleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
      await selectionRuleInput.setValue('{ #el ...}');

      expect(testComponent.apiPlanForm.hasNextStep()).toEqual(true);
      expect(testComponent.apiPlanForm.hasPreviousStep()).toEqual(true);
      testComponent.apiPlanForm.nextStep();

      // 3- Restriction Step

      const rateLimitEnabledInput = await planForm.getRateLimitEnabledInput();
      await rateLimitEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('rate-limit', {});

      const quotaEnabledInput = await planForm.getQuotaEnabledInput();
      await quotaEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('quota', {});

      const resourceFilteringEnabledInput = await planForm.getResourceFilteringEnabledInput();
      await resourceFilteringEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('resource-filtering', {});

      expect(testComponent.apiPlanForm.hasNextStep()).toEqual(false);
      expect(testComponent.apiPlanForm.hasPreviousStep()).toEqual(true);

      expect(testComponent.planControl.touched).toEqual(true);
      expect(testComponent.planControl.dirty).toEqual(true);
      expect(testComponent.planControl.valid).toEqual(true);
      expect(testComponent.planControl.value).toEqual({
        name: '🗺',
        description: 'Description',
        characteristics: ['C1'],
        comment_message: 'Comment message',
        comment_required: true,
        excluded_groups: ['group-a'],
        general_conditions: 'doc-1',
        security: 'JWT',
        securityDefinition: {},
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
    });

    it('should add new  OAuth2 plan', async () => {
      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, []);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([]);
      planForm.httpRequest(httpTestingController).expectTagsListRequest([]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('🗺');

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      await securityTypeInput.clickOptions({ text: /OAuth2/ });
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('oauth2', {});
      planForm.httpRequest(httpTestingController).expectResourceGetRequest();

      // 3- Restriction Step
      // Skip

      expect(testComponent.planControl.touched).toEqual(true);
      expect(testComponent.planControl.dirty).toEqual(true);
      expect(testComponent.planControl.valid).toEqual(true);
      expect(testComponent.planControl.value).toEqual({
        name: '🗺',
        description: '',
        characteristics: [],
        comment_message: '',
        comment_required: false,
        excluded_groups: [],
        general_conditions: '',
        tags: [],
        security: 'OAUTH2',
        securityDefinition: {},
        selection_rule: null,
        validation: 'MANUAL',
        flows: [
          {
            enabled: true,
            'path-operator': {
              operator: 'STARTS_WITH',
              path: '/',
            },
            post: [],
            pre: [],
          },
        ],
      });
    });
  });

  describe('Create mode V4 without API', () => {
    beforeEach(async () => {
      configureTestingModule('create');
      fixture.detectChanges();
    });

    it('should add new plan', async () => {
      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      await nameInput.setValue('🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      await descriptionInput.setValue('Description');

      const characteristicsInput = await planForm.getCharacteristicsInput();
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      expect(generalConditionsInput).toEqual(null);

      const validationToggle = await planForm.getValidationToggle();
      await validationToggle.toggle();

      const commentRequired = await planForm.getCommentRequiredToggle();
      await commentRequired.toggle();

      const commentMessageInput = await planForm.getCommentMessageInput();
      await commentMessageInput.setValue('Comment message');

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(shardingTagsInput).toEqual(null);

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      await securityTypeInput.clickOptions({ text: /JWT/ });
      await securityTypeInput.getOptions({ text: /OAuth2/ }).then((options) => expect(options.length).toBe(0));
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('jwt', {});

      const selectionRuleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
      await selectionRuleInput.setValue('{ #el ...}');

      // 3- Restriction Step

      const rateLimitEnabledInput = await planForm.getRateLimitEnabledInput();
      await rateLimitEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('rate-limit', {});

      const quotaEnabledInput = await planForm.getQuotaEnabledInput();
      await quotaEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('quota', {});

      const resourceFilteringEnabledInput = await planForm.getResourceFilteringEnabledInput();
      await resourceFilteringEnabledInput.toggle();
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('resource-filtering', {});

      expect(testComponent.planControl.touched).toEqual(true);
      expect(testComponent.planControl.dirty).toEqual(true);
      expect(testComponent.planControl.valid).toEqual(true);
      expect(testComponent.planControl.value).toEqual({
        name: '🗺',
        description: 'Description',
        characteristics: ['C1'],
        comment_message: 'Comment message',
        comment_required: true,
        excluded_groups: ['group-a'],
        general_conditions: '',
        tags: [],
        security: {
          configuration: {},
          type: 'JWT',
        },
        selection_rule: '{ #el ...}',
        validation: 'auto',
        flows: [
          {
            selectors: [
              {
                type: 'http',
                path: '/',
                pathOperator: 'STARTS_WITH',
              },
            ],
            enabled: true,
            request: [
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
    });
  });

  describe('Edit mode V3', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV3({
      tags: [TAG_1_ID],
    });
    const PLAN_ID = 'plan-1';

    beforeEach(async () => {
      configureTestingModule('edit', API);
    });

    it('should edit plan', async () => {
      const planToUpdate = fakePlanV3({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
      testComponent.planControl = new FormControl(planToUpdate);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      expect(await nameInput.getValue()).toBe('Old 🗺');
      await nameInput.setValue('🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      expect(await descriptionInput.getValue()).toBe('Old Description');
      await descriptionInput.setValue('Description');

      const characteristicsInput = await planForm.getCharacteristicsInput();
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      await generalConditionsInput.clickOptions({ text: 'Doc 1' });

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      await shardingTagsInput.clickOptions({ text: /Tag 1/ }); // Unselect Tag 1
      await shardingTagsInput.getOptions({ text: /Tag 2/ }).then((options) => options[0].isDisabled());

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      expect(await securityTypeInput.isDisabled()).toEqual(true);
      expect(await securityTypeInput.getValueText()).toEqual('Keyless (public)');

      const selectionRuleInput = await planForm.getSelectionRuleInput();
      expect(selectionRuleInput).toEqual(null); // no selection rule for keyless

      // Save
      expect(testComponent.planControl.touched).toEqual(true);
      expect(testComponent.planControl.dirty).toEqual(true);
      expect(testComponent.planControl.valid).toEqual(true);
      expect(testComponent.planControl.value).toEqual({
        name: '🗺',
        description: 'Description',
        characteristics: ['C1'],
        tags: [],
        validation: 'MANUAL',
        comment_message: undefined,
        comment_required: false,
        excluded_groups: ['group-a'],
        selection_rule: undefined,
        security: 'KEY_LESS',
        securityDefinition: '{}',
        general_conditions: 'doc-1',
      });
    });

    it('should display api-key plan', async () => {
      const planToUpdate = fakePlanV3({
        id: PLAN_ID,
        name: 'Old 🗺',
        description: 'Old Description',
        security: PlanSecurityType.API_KEY,
        securityDefinition: { propagateApiKey: true },
        tags: [TAG_1_ID],
      });
      testComponent.planControl = new FormControl(planToUpdate);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('api-key', fakeApiKeySchema);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      expect(await nameInput.getValue()).toBe('Old 🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      expect(await descriptionInput.getValue()).toBe('Old Description');

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      expect(await securityTypeInput.isDisabled()).toEqual(true);
      expect(await securityTypeInput.getValueText()).toEqual('API Key');

      // Expect the security config value to be completed correctly
      const jsonSchemaPropagateApiKeyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[id*="propagateApiKey"]' }));
      expect(await jsonSchemaPropagateApiKeyToggle.isChecked()).toEqual(true);

      const selectionRuleInput = await planForm.getSelectionRuleInput();
      expect(selectionRuleInput).toBeDefined();
    });
  });

  describe('Edit mode V3 with disabled control', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV3({
      tags: [TAG_1_ID],
      definition_context: { origin: 'kubernetes' },
    });
    const PLAN_ID = 'plan-1';

    beforeEach(async () => {
      configureTestingModule('edit', API);
    });

    it('should access plan in read only ', async () => {
      const planToUpdate = fakePlanV3({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
      testComponent.planControl = new FormControl({
        value: planToUpdate,
        disabled: true,
      });
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      expect(await nameInput.getValue()).toBe('Old 🗺');
      expect(await nameInput.isDisabled()).toBe(true);

      const descriptionInput = await planForm.getDescriptionInput();
      expect(await descriptionInput.getValue()).toBe('Old Description');
      expect(await descriptionInput.isDisabled()).toBe(true);

      const characteristicsInput = await planForm.getCharacteristicsInput();
      expect(await characteristicsInput.isDisabled()).toBe(true);

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      expect(await generalConditionsInput.isDisabled()).toBe(true);

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      expect(await shardingTagsInput.isDisabled()).toBe(true);

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      expect(await excludedGroupsInput.isDisabled()).toBe(true);

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      expect(await securityTypeInput.isDisabled()).toEqual(true);

      const selectionRuleInput = await planForm.getSelectionRuleInput();
      expect(selectionRuleInput).toEqual(null); // no selection rule for keyless
    });
  });

  describe('Edit mode V4', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV4({
      tags: [TAG_1_ID],
    });
    const PLAN_ID = 'plan-1';

    beforeEach(async () => {
      configureTestingModule('edit', API);
    });

    it('should edit plan', async () => {
      const planToUpdate = fakePlanV4({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
      testComponent.planControl = new FormControl(planToUpdate);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      expect(await nameInput.getValue()).toBe('Old 🗺');
      await nameInput.setValue('🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      expect(await descriptionInput.getValue()).toBe('Old Description');
      await descriptionInput.setValue('Description');

      const characteristicsInput = await planForm.getCharacteristicsInput();
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      await generalConditionsInput.clickOptions({ text: 'Doc 1' });

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      await shardingTagsInput.clickOptions({ text: /Tag 1/ }); // Unselect Tag 1
      await shardingTagsInput.getOptions({ text: /Tag 2/ }).then((options) => options[0].isDisabled());

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      expect(await securityTypeInput.isDisabled()).toEqual(true);
      expect(await securityTypeInput.getValueText()).toEqual('Keyless (public)');

      const selectionRuleInput = await planForm.getSelectionRuleInput();
      expect(selectionRuleInput).toEqual(null); // no selection rule for keyless

      // Save
      expect(testComponent.planControl.touched).toEqual(true);
      expect(testComponent.planControl.dirty).toEqual(true);
      expect(testComponent.planControl.valid).toEqual(true);
      expect(testComponent.planControl.value).toEqual({
        name: '🗺',
        description: 'Description',
        characteristics: ['C1'],
        tags: [],
        validation: 'manual',
        comment_message: undefined,
        comment_required: false,
        excluded_groups: ['group-a'],
        security: {
          configuration: {},
          type: 'KEY_LESS',
        },
        general_conditions: 'doc-1',
      });
    });

    it('should display api-key plan', async () => {
      const planToUpdate = fakePlanV4({
        id: PLAN_ID,
        name: 'Old 🗺',
        description: 'Old Description',
        security: {
          type: PlanSecurityType.API_KEY,
          configuration: { propagateApiKey: true },
        },
        tags: [TAG_1_ID],
      });
      testComponent.planControl = new FormControl(planToUpdate);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupLisRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
      planForm.httpRequest(httpTestingController).expectPolicySchemaGetRequest('api-key', fakeApiKeySchema);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      // 1- General Step
      const nameInput = await planForm.getNameInput();
      expect(await nameInput.getValue()).toBe('Old 🗺');

      const descriptionInput = await planForm.getDescriptionInput();
      expect(await descriptionInput.getValue()).toBe('Old Description');

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');

      // 2- Secure Step
      const securityTypeInput = await planForm.getSecurityTypeInput();
      expect(await securityTypeInput.isDisabled()).toEqual(true);
      expect(await securityTypeInput.getValueText()).toEqual('API Key');

      // Expect the security config value to be completed correctly
      const jsonSchemaPropagateApiKeyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[id*="propagateApiKey"]' }));
      expect(await jsonSchemaPropagateApiKeyToggle.isChecked()).toEqual(true);

      const selectionRuleInput = await planForm.getSelectionRuleInput();
      expect(selectionRuleInput).toBeDefined();
    });
  });
});
