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
import { PortalSettings } from 'src/entities/portal/portalSettings';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { ApiQualityRulesComponent } from './api-quality-rules.component';
import { ApiQualityRulesModule } from './api-quality-rules.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { QualityRule } from '../../../entities/qualityRule';

describe('ApiQualityRulesComponent', () => {
  let fixture: ComponentFixture<ApiQualityRulesComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const portalSettingsMock: PortalSettings = {
    apiQualityMetrics: {
      enabled: true,
      descriptionWeight: 100,
      descriptionMinLength: 100,
      logoWeight: 100,
      categoriesWeight: 100,
      labelsWeight: 100,
      functionalDocumentationWeight: 100,
      technicalDocumentationWeight: 100,
      healthcheckWeight: 100,
    },
    apiReview: {
      enabled: false,
    },
    apiScore: {
      enabled: false,
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiQualityRulesModule, MatIconTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-quality_rule-u',
            'environment-quality_rule-d',
            'environment-quality_rule-c',
            'environment-quality_rule-r',
          ],
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiQualityRulesComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Api Quality rules details', () => {
    it('should edit Api Quality rules details', async () => {
      const qualityRules: QualityRule[] = [
        { id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 },
        { id: 'test_id1', name: 'test_name2', description: 'description2', weight: 2 },
      ];
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const enableApiReviewToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[data-testid=api-review-enabled-toggle]' }),
      );
      expect(await enableApiReviewToggle.isChecked()).toBe(false);
      await enableApiReviewToggle.toggle();

      const technicalDocumentationWeightInput = await loader.getHarness(
        MatInputHarness.with({ selector: '[formControlName=technicalDocumentationWeight]' }),
      );
      await technicalDocumentationWeightInput.setValue('500');
      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        apiQualityMetrics: {
          enabled: true,
          categoriesWeight: 100,
          descriptionMinLength: 100,
          descriptionWeight: 100,
          functionalDocumentationWeight: 100,
          healthcheckWeight: 100,
          labelsWeight: 100,
          logoWeight: 100,
          technicalDocumentationWeight: 500,
        },
        apiReview: {
          enabled: false,
        },
        apiScore: {
          enabled: false,
        },
      });
    });
  });

  describe('Manual rules table', () => {
    it('should display manual rules table', async () => {
      const qualityRules: QualityRule[] = [
        { id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 },
        { id: 'test_id1', name: 'test_name2', description: 'description2', weight: 2 },
      ];
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiQualityRulesTable' }));
      expect(await table.getCellTextByIndex()).toEqual([
        ['test_name1', 'description1', '1', ''],
        ['test_name2', 'description2', '2', ''],
      ]);
    });

    it('should delete rule', async () => {
      const qualityRules: QualityRule[] = [
        { id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 },
        { id: 'test_id1', name: 'test_name2', description: 'description2', weight: 2 },
      ];
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Delete manual rule"]` }));
      await button.click();

      const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
      await confirmDialog.confirm();
      expectDeleteManualRuleRequest('test_id1');
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);
    });

    it('should update rule', async () => {
      const qualityRules: QualityRule[] = [
        { id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 },
        { id: 'test_id1', name: 'test_name2', description: 'description2', weight: 2 },
      ];
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiQualityRulesTable' }));
      expect((await table.getRows()).length).toEqual(2);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Edit manual rule"]` }));
      await button.click();

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeFalsy();

      const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Test quality rule');

      await submitButton.click();
      expect(qualityRules[1].name).toEqual('test_name2');
      expectUpdateManualRuleRequest('test_id1');
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);
    });

    it('should add new quality rule', async () => {
      const qualityRules: QualityRule[] = [
        { id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 },
        { id: 'test_id1', name: 'test_name2', description: 'description2', weight: 2 },
      ];
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiQualityRulesTable' }));
      expect((await table.getRows()).length).toEqual(2);

      const button = await loader.getHarness(MatButtonHarness.with({ selector: `[aria-label="Add new quality rule"]` }));
      await button.click();

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Test quality rule name');

      const descriptionInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      await descriptionInput.setValue('Test quality rule description');

      const weightInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=weight]' }));
      await weightInput.setValue('1');

      await submitButton.click();
      expectCreateManualRuleRequest({ id: 'test_id1', name: 'test_name1', description: 'description1', weight: 1 });
      expectListQualityRulesRequest(qualityRules);
      expectGetPortalSettingsRequest(portalSettingsMock);
    });
  });

  function expectGetPortalSettingsRequest(portalSettings: PortalSettings) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/settings`,
        method: 'GET',
      })
      .flush(portalSettings);
  }

  function expectListQualityRulesRequest(qualityRuleList: QualityRule[]) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules`,
        method: 'GET',
      })
      .flush(qualityRuleList);
  }

  function expectDeleteManualRuleRequest(qualityRuleId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules/${qualityRuleId}`,
        method: 'DELETE',
      })
      .flush(qualityRuleId);
  }

  function expectUpdateManualRuleRequest(qualityRuleId: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules/${qualityRuleId}`,
        method: 'PUT',
      })
      .flush(qualityRuleId);
  }

  function expectCreateManualRuleRequest(newQualityRule: QualityRule) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules`,
        method: 'POST',
      })
      .flush(newQualityRule);
  }
});
