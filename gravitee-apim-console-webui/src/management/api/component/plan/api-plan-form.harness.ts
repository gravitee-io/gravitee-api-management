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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';

import { Tag } from '../../../../entities/tag/tag';
import { CONSTANTS_TESTING } from '../../../../shared/testing';
import { Group } from '../../../../entities/group/group';
import { Page } from '../../../../entities/page';

export class ApiPlanFormHarness extends ComponentHarness {
  static hostSelector = 'api-plan-form';

  // 1- General Step
  public getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  public getDescriptionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="description"]' }));
  public getCharacteristicsInput = this.locatorFor(GioFormTagsInputHarness.with({ selector: '[formControlName="characteristics"]' }));
  public getCharacteristicsField = this.locatorFor(MatFormFieldHarness.with({ floatingLabelText: 'Characteristics' }));
  public getGeneralConditionsInput = this.locatorForOptional(MatSelectHarness.with({ selector: '[formControlName="generalConditions"]' }));
  public getValidationToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="autoValidation"]' }));
  public validationTogglePresent(): Promise<boolean> {
    return this.getValidationToggle()
      .then(_ => true)
      .catch(_ => false);
  }
  public getCommentRequiredToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="commentRequired"]' }));
  public commentRequiredTogglePresent(): Promise<boolean> {
    return this.getCommentRequiredToggle()
      .then(_ => true)
      .catch(_ => false);
  }
  public getCommentMessageInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="commentMessage"]' }));
  public getShardingTagsInput = this.locatorForOptional(MatSelectHarness.with({ selector: '[formControlName="shardingTags"]' }));
  public getExcludedGroupsInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="excludedGroups"]' }));

  // 2- Secure Step
  public getSelectionRuleInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="selectionRule"]' }));

  // 3- Restriction Step
  public getRateLimitEnabledInput = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="rateLimitEnabled"]' }));
  public getQuotaEnabledInput = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="quotaEnabled"]' }));
  public getResourceFilteringEnabledInput = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[formControlName="resourceFilteringEnabled"]' }),
  );

  httpRequest(httpTestingController: HttpTestingController) {
    function expectTagsListRequest(tags: Tag[] = []) {
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
        })
        .flush(tags);
    }

    function expectGroupListRequest(groups: Group[] = []) {
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups`,
        })
        .flush(groups);
    }

    function expectDocumentationSearchRequest(apiId: string, pages: Page[] = []) {
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/pages?type=MARKDOWN&api=${apiId}`,
        })
        .flush(pages);
    }

    function expectCurrentUserTagsRequest(tags: string[]) {
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/user/tags`,
        })
        .flush(tags);
    }

    function expectPolicySchemaGetRequest(type: string, schema: unknown) {
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/policies/${type}/schema`, method: 'GET' }).flush(schema);
    }

    function expectPolicySchemaV2GetRequest(type: string, schema: unknown) {
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/${type}/schema`, method: 'GET' })
        .flush(schema);
    }

    return {
      expectTagsListRequest,
      expectGroupListRequest,
      expectDocumentationSearchRequest,
      expectCurrentUserTagsRequest,
      expectPolicySchemaGetRequest,
      expectPolicySchemaV2GetRequest,
    };
  }
}
