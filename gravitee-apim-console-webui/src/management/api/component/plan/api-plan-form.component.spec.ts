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
import { MatStepHarness } from '@angular/material/stepper/testing';

import { ApiPlanFormModule } from './api-plan-form.module';
import { ApiPlanFormHarness } from './api-plan-form.harness';
import { ApiPlanFormComponent } from './api-plan-form.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { fakeTag } from '../../../../entities/tag/tag.fixture';
import {
  Api,
  ApiType,
  CreatePlanV2,
  CreatePlanV4,
  fakeApiV2,
  fakeApiV4,
  fakePlanV2,
  fakePlanV4,
  fakeProxyTcpApiV4,
  Plan,
  PlanV2,
  PlanV4,
} from '../../../../entities/management-api-v2';
import { AVAILABLE_PLANS_FOR_MENU, PlanFormType, PlanMenuItemVM } from '../../../../services-ngx/constants.service';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../entities/Constants';
import { isApiV4 } from '../../../../util';

@Component({
  template: `
    <api-plan-form
      #apiPlanForm
      [formControl]="planControl"
      [mode]="mode"
      [api]="api"
      [apiType]="apiType"
      [planMenuItem]="planMenuItem"
      [isTcpApi]="isTcpApi"
    ></api-plan-form>
  `,
})
class TestComponent {
  @ViewChild('apiPlanForm') apiPlanForm: ApiPlanFormComponent;
  mode: 'create' | 'edit' = 'create';
  planMenuItem: PlanMenuItemVM;
  planControl = new FormControl();
  api?: Api;
  apiType?: ApiType;
  plan?: Plan;
  isTcpApi?: boolean = false;
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
  let fixture: ComponentFixture<TestComponent>;
  let testComponent: TestComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const configureTestingModule = (mode: 'create' | 'edit', planFormType: PlanFormType, api?: Api, apiType?: ApiType) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ReactiveFormsModule, NoopAnimationsModule, GioTestingModule, ApiPlanFormModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: ['api-plan-u'] },
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              oauth2: { enabled: false },
              jwt: { enabled: true },
              mtls: { enabled: true },
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
    testComponent.planMenuItem = AVAILABLE_PLANS_FOR_MENU.find((vm) => vm.planFormType === planFormType);
    testComponent.api = api;
    testComponent.apiType = apiType;
    testComponent.isTcpApi = isApiV4(api) && api.listeners.find((listener) => listener.type === 'TCP') != null;
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create mode V2 with API', () => {
    describe('OAuth2 plan', () => {
      const TAG_1_ID = 'tag-1';
      const API = fakeApiV2({
        tags: [TAG_1_ID],
        resources: [
          {
            name: 'OAuth2 AM Resource',
            enabled: true,
            type: 'OAUTH2',
            configuration: '',
          },
        ],
      });
      beforeEach(async () => {
        configureTestingModule('create', 'OAUTH2', API);
        fixture.detectChanges();
      });

      it('should be added', async () => {
        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
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
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('oauth2', {});

        // 3- Restriction Step
        // Skip

        expect(testComponent.planControl.touched).toEqual(true);
        expect(testComponent.planControl.dirty).toEqual(true);
        expect(testComponent.planControl.valid).toEqual(true);
        expect(testComponent.planControl.value).toEqual({
          name: '🗺',
          description: '',
          characteristics: [],
          commentMessage: '',
          commentRequired: false,
          excludedGroups: [],
          generalConditions: '',
          tags: [],
          security: {
            type: 'OAUTH2',
            configuration: {},
          },
          selectionRule: null,
          validation: 'MANUAL',
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
      });
    });
    describe('JWT plan', () => {
      const TAG_1_ID = 'tag-1';
      const API = fakeApiV2({
        tags: [TAG_1_ID],
        resources: [
          {
            name: 'OAuth2 AM Resource',
            enabled: true,
            type: 'oauth2-am',
            configuration: '',
          },
        ],
      });

      beforeEach(async () => {
        configureTestingModule('create', 'JWT', API);
        fixture.detectChanges();
      });

      it('should be added', async () => {
        loader = TestbedHarnessEnvironment.loader(fixture);
        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm
          .httpRequest(httpTestingController)
          .expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1', published: true }]);
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
        await testComponent.apiPlanForm.waitForNextStep();
        fixture.detectChanges();

        // 2- Secure Step
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('jwt', {});

        const selectionRuleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));
        await selectionRuleInput.setValue('{ #el ...}');

        expect(testComponent.apiPlanForm.hasNextStep()).toEqual(true);
        expect(testComponent.apiPlanForm.hasPreviousStep()).toEqual(true);
        await testComponent.apiPlanForm.waitForNextStep();

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
          commentMessage: 'Comment message',
          commentRequired: true,
          excludedGroups: ['group-a'],
          generalConditions: 'doc-1',
          security: {
            type: 'JWT',
            configuration: {},
          },
          selectionRule: '{ #el ...}',
          tags: [TAG_1_ID],
          validation: 'AUTO',
          flows: [
            {
              enabled: true,
              pathOperator: {
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
        } as CreatePlanV2);
      });
    });
    describe('API Key plan', () => {
      const TAG_1_ID = 'tag-1';
      const API = fakeApiV2({
        tags: [TAG_1_ID],
        resources: [
          {
            name: 'OAuth2 AM Resource',
            enabled: true,
            type: 'oauth2-am',
            configuration: '',
          },
        ],
      });

      beforeEach(async () => {
        configureTestingModule('create', 'API_KEY', API);
        fixture.detectChanges();
      });

      it('should be added', async () => {
        loader = TestbedHarnessEnvironment.loader(fixture);
        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm
          .httpRequest(httpTestingController)
          .expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1', published: true }]);
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
        await testComponent.apiPlanForm.waitForNextStep();
        fixture.detectChanges();

        // 2- Secure Step
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('api-key', fakeApiKeySchema);

        const jsonSchemaPropagateApiKeyToggle = await loader.getHarness(
          MatSlideToggleHarness.with({ selector: '[id*="propagateApiKey"]' }),
        );
        expect(await jsonSchemaPropagateApiKeyToggle.isChecked()).toEqual(false);
        await jsonSchemaPropagateApiKeyToggle.toggle();

        expect(testComponent.apiPlanForm.hasNextStep()).toEqual(true);
        expect(testComponent.apiPlanForm.hasPreviousStep()).toEqual(true);
        await testComponent.apiPlanForm.waitForNextStep();

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
          commentMessage: 'Comment message',
          commentRequired: true,
          excludedGroups: ['group-a'],
          generalConditions: 'doc-1',
          security: {
            type: 'API_KEY',
            configuration: {
              propagateApiKey: true,
            },
          },
          selectionRule: null,
          tags: [TAG_1_ID],
          validation: 'AUTO',
          flows: [
            {
              enabled: true,
              pathOperator: {
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
        } as CreatePlanV2);
      });
    });
    describe('API Keyless plan', () => {
      const TAG_1_ID = 'tag-1';
      const API = fakeApiV2({
        tags: [TAG_1_ID],
        resources: [
          {
            name: 'OAuth2 AM Resource',
            enabled: true,
            type: 'oauth2-am',
            configuration: '',
          },
        ],
      });

      beforeEach(async () => {
        configureTestingModule('create', 'KEY_LESS', API);
        fixture.detectChanges();
      });
      it('should be created', async () => {
        loader = TestbedHarnessEnvironment.loader(fixture);
        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm
          .httpRequest(httpTestingController)
          .expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1', published: true }]);
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

        expect(await planForm.validationTogglePresent()).toEqual(false);
        expect(await planForm.commentRequiredTogglePresent()).toEqual(false);

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
        // DISABLED

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
          commentMessage: '',
          commentRequired: false,
          excludedGroups: ['group-a'],
          generalConditions: 'doc-1',
          security: {
            type: 'KEY_LESS',
            configuration: {},
          },
          selectionRule: null,
          tags: [TAG_1_ID],
          validation: 'MANUAL',
          flows: [
            {
              enabled: true,
              pathOperator: {
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
        } as CreatePlanV2);
      });
    });
  });

  describe('Create mode V4', () => {
    it('should add new plan without API', async () => {
      configureTestingModule('create', 'JWT', undefined, 'MESSAGE');
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
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
      planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('jwt', {});

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
        commentMessage: 'Comment message',
        commentRequired: true,
        excludedGroups: ['group-a'],
        generalConditions: '',
        tags: [],
        mode: 'STANDARD',
        security: {
          configuration: {},
          type: 'JWT',
        },
        selectionRule: '{ #el ...}',
        validation: 'AUTO',
        flows: [
          {
            selectors: [
              {
                type: 'CHANNEL',
                channel: '/',
                channelOperator: 'STARTS_WITH',
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
      } as CreatePlanV4);
    });
    it('should add new plan with API', async () => {
      const API = fakeApiV4({ type: 'MESSAGE' });
      configureTestingModule('create', 'JWT', API);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
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

      const descriptionInput = await planForm.getDescriptionInput();
      await descriptionInput.setValue('Description');

      const characteristicsInput = await planForm.getCharacteristicsInput();
      await characteristicsInput.addTag('C1');

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      expect(await generalConditionsInput.getValueText()).toEqual('');

      const validationToggle = await planForm.getValidationToggle();
      await validationToggle.toggle();

      const commentRequired = await planForm.getCommentRequiredToggle();
      await commentRequired.toggle();

      const commentMessageInput = await planForm.getCommentMessageInput();
      await commentMessageInput.setValue('Comment message');

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toEqual('');

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      await excludedGroupsInput.clickOptions({ text: 'Group A' });

      // 2- Secure Step
      planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('jwt', {});

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
        commentMessage: 'Comment message',
        commentRequired: true,
        excludedGroups: ['group-a'],
        generalConditions: '',
        tags: [],
        mode: 'STANDARD',
        security: {
          configuration: {},
          type: 'JWT',
        },
        selectionRule: '{ #el ...}',
        validation: 'AUTO',
        flows: [
          {
            selectors: [
              {
                type: 'CHANNEL',
                channel: '/',
                channelOperator: 'STARTS_WITH',
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
      } as CreatePlanV4);
    });
  });
  it('should not display secure step with push plans', async () => {
    configureTestingModule('create', 'PUSH', undefined, 'MESSAGE');
    fixture.detectChanges();

    const planForm = await loader.getHarness(ApiPlanFormHarness);

    planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
    fixture.detectChanges();

    expect(testComponent.planControl.touched).toEqual(false);
    expect(testComponent.planControl.dirty).toEqual(false);
    expect(testComponent.planControl.valid).toEqual(false);

    const stepsHarness = await loader.getAllHarnesses(MatStepHarness);
    const steps = await Promise.all(stepsHarness.map((step) => step.getLabel()));
    expect(steps).toEqual(['General', 'Restriction']);
  });

  describe('Edit mode V2', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV2({
      tags: [TAG_1_ID],
    });
    const PLAN_ID = 'plan-1';

    beforeEach(async () => {
      configureTestingModule('edit', 'KEY_LESS', API);
    });

    it('should edit plan', async () => {
      const planToUpdate = fakePlanV2({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
      testComponent.planControl = new FormControl(planToUpdate);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm
        .httpRequest(httpTestingController)
        .expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1', published: true }]);
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
        commentMessage: 'comment message',
        commentRequired: false,
        excludedGroups: ['group-a'],
        selectionRule: undefined,
        security: {
          type: 'KEY_LESS',
          configuration: {},
        },
        generalConditions: 'doc-1',
      } as PlanV2);
    });
  });

  describe('Edit mode V2 with disabled control', () => {
    const TAG_1_ID = 'tag-1';
    const API = fakeApiV2({
      tags: [TAG_1_ID],
      definitionContext: { origin: 'KUBERNETES' },
    });
    const PLAN_ID = 'plan-1';

    beforeEach(async () => {
      configureTestingModule('edit', 'KEY_LESS', API);
    });

    it('should access plan in read only ', async () => {
      const planToUpdate = fakePlanV2({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
      testComponent.planControl = new FormControl({
        value: planToUpdate,
        disabled: true,
      });
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm
        .httpRequest(httpTestingController)
        .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
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

      const characteristicsInput = await planForm.getCharacteristicsField();
      expect(await characteristicsInput.isDisabled()).toBe(true);

      const generalConditionsInput = await planForm.getGeneralConditionsInput();
      expect(await generalConditionsInput.isDisabled()).toBe(true);

      const shardingTagsInput = await planForm.getShardingTagsInput();
      expect(await shardingTagsInput.getValueText()).toContain('Tag 1');
      expect(await shardingTagsInput.isDisabled()).toBe(true);

      const excludedGroupsInput = await planForm.getExcludedGroupsInput();
      expect(await excludedGroupsInput.isDisabled()).toBe(true);

      // 2- Secure Step

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

    describe('Keyless plan', () => {
      it('should edit plan', async () => {
        configureTestingModule('edit', 'KEY_LESS', API);
        const planToUpdate = fakePlanV4({ id: PLAN_ID, name: 'Old 🗺', description: 'Old Description', tags: [TAG_1_ID] });
        testComponent.planControl = new FormControl(planToUpdate);
        fixture.detectChanges();

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm
          .httpRequest(httpTestingController)
          .expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1', published: true }]);
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
          commentMessage: 'comment message',
          commentRequired: false,
          excludedGroups: ['group-a'],
          mode: 'STANDARD',
          security: {
            type: 'KEY_LESS',
            configuration: {},
          },
          generalConditions: 'doc-1',
          selectionRule: undefined,
        } as PlanV4);
      });

      it('should only show published pages for general conditions', async () => {
        configureTestingModule('edit', 'KEY_LESS', API);
        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm
          .httpRequest(httpTestingController)
          .expectTagsListRequest([fakeTag({ id: TAG_1_ID, name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [
          { id: 'doc-1', name: 'Doc 1', published: true },
          { id: 'doc-2', name: 'Doc 2', published: false },
        ]);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);

        const generalConditionsInput = await planForm.getGeneralConditionsInput();
        await generalConditionsInput.open();
        const options = await generalConditionsInput.getOptions();
        expect(options.length).toEqual(2);
        expect(await options[0].getText()).toEqual(''); // First option is blank so user can choose not to have general conditions
        expect(await options[1].getText()).toEqual('Doc 1');
      });

      it('should not display restrictions step for TCP api', async () => {
        const tcpApi = fakeProxyTcpApiV4();
        configureTestingModule('create', 'KEY_LESS', tcpApi, 'PROXY');
        fixture.detectChanges();

        const planForm = await loader.getHarness(ApiPlanFormHarness);

        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(tcpApi.id, []);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([]);
        planForm.httpRequest(httpTestingController).expectTagsListRequest([]);
        fixture.detectChanges();

        expect(testComponent.planControl.touched).toEqual(false);
        expect(testComponent.planControl.dirty).toEqual(false);
        expect(testComponent.planControl.valid).toEqual(false);

        const stepsHarness = await loader.getAllHarnesses(MatStepHarness);
        const steps = await Promise.all(stepsHarness.map((step) => step.getLabel()));
        expect(steps).toEqual(['General']);
      });
    });
    describe('API Key plan', () => {
      beforeEach(async () => {
        configureTestingModule('edit', 'API_KEY', API);
      });
      it('should display api-key plan', async () => {
        const planToUpdate = fakePlanV4({
          id: PLAN_ID,
          name: 'Old 🗺',
          description: 'Old Description',
          security: {
            type: 'API_KEY',
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
        planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(API.id, [{ id: 'doc-1', name: 'Doc 1' }]);
        planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([TAG_1_ID]);
        planForm.httpRequest(httpTestingController).expectPolicySchemaV2GetRequest('api-key', fakeApiKeySchema);
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

        // Expect the security config value to be completed correctly
        const jsonSchemaPropagateApiKeyToggle = await loader.getHarness(
          MatSlideToggleHarness.with({ selector: '[id*="propagateApiKey"]' }),
        );
        expect(await jsonSchemaPropagateApiKeyToggle.isChecked()).toEqual(true);

        const selectionRuleInput = await planForm.getSelectionRuleInput();
        expect(selectionRuleInput).toBeDefined();
      });
    });
  });
  describe('MTLS plans', () => {
    it.each(['PROXY', 'MESSAGE'])('should display secure step with mTLS plans for %s api', async (apiType: ApiType) => {
      configureTestingModule('create', 'MTLS', undefined, apiType);
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      const stepsHarness = await loader.getAllHarnesses(MatStepHarness);
      const steps = await Promise.all(stepsHarness.map((step) => step.getLabel()));
      expect(steps).toEqual(['General', 'mTLS authentication configuration', 'Restriction']);
    });

    it('should not display secure step with mTLS plans for TCP api', async () => {
      const tcpApi = fakeProxyTcpApiV4();
      configureTestingModule('create', 'MTLS', tcpApi, 'PROXY');
      fixture.detectChanges();

      const planForm = await loader.getHarness(ApiPlanFormHarness);

      planForm.httpRequest(httpTestingController).expectGroupListRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      planForm.httpRequest(httpTestingController).expectDocumentationSearchRequest(tcpApi.id, []);
      planForm.httpRequest(httpTestingController).expectCurrentUserTagsRequest([]);
      planForm.httpRequest(httpTestingController).expectTagsListRequest([]);
      fixture.detectChanges();

      expect(testComponent.planControl.touched).toEqual(false);
      expect(testComponent.planControl.dirty).toEqual(false);
      expect(testComponent.planControl.valid).toEqual(false);

      const stepsHarness = await loader.getAllHarnesses(MatStepHarness);
      const steps = await Promise.all(stepsHarness.map((step) => step.getLabel()));
      expect(steps).toEqual(['General']);
    });
  });
});
