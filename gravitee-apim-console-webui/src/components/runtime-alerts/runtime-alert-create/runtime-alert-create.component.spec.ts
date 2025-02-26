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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { LOCALE_ID } from '@angular/core';

import { RuntimeAlertCreateComponent } from './runtime-alert-create.component';
import { RuntimeAlertCreateModule } from './runtime-alert-create.module';
import { RuntimeAlertCreateHarness } from './runtime-alert-create.harness';
import { RuntimeAlertCreateTimeframeHarness } from './components/runtime-alert-create-timeframe/runtime-alert-create-timeframe.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Days } from '../../../entities/alerts/notificationPeriod';
import { NewAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';

describe('RuntimeAlertCreateComponent', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envId';

  const fillGeneralForm = async (index: number) => {
    const expectedRules = [
      'Alert when a metric of the request validates a condition',
      'Alert when there is no request matching filters received for a period of time',
      'Alert when the aggregated value of a request metric rises a threshold',
      'Alert when the rate of a given condition rises a threshold',
      'Alert when the health status of an endpoint has changed',
    ];
    const expectedSeverities = ['info', 'warning', 'critical'];
    const generalForm = await componentHarness.getGeneralFormHarness();
    await generalForm.setName('alert');
    await generalForm.toggleEnabled();
    await generalForm.selectSeverity(expectedSeverities[1]);
    await generalForm.selectRule(expectedRules[index]);
  };

  let fixture: ComponentFixture<RuntimeAlertCreateComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: RuntimeAlertCreateHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [RuntimeAlertCreateComponent],
      imports: [NoopAnimationsModule, MatIconTestingModule, RuntimeAlertCreateModule, GioTestingModule],
      providers: [
        { provide: LOCALE_ID, useValue: 'en-GB' },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID, envId: ENVIRONMENT_ID },
              data: { referenceType: 'API' },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RuntimeAlertCreateComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, RuntimeAlertCreateHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should fill the general form', async () => {
    const expectedRules = [
      'Alert when a metric of the request validates a condition',
      'Alert when there is no request matching filters received for a period of time',
      'Alert when the aggregated value of a request metric rises a threshold',
      'Alert when the rate of a given condition rises a threshold',
      'Alert when the health status of an endpoint has changed',
    ];
    const expectedSeverities = ['info', 'warning', 'critical'];
    const generalForm = await componentHarness.getGeneralFormHarness();
    expect(await generalForm.getRulesOptions()).toHaveLength(5);
    expect(await generalForm.getRulesOptions()).toStrictEqual(expectedRules);
    expect(await generalForm.getSeverityOptions()).toStrictEqual(expectedSeverities);

    await generalForm.setName('alert');
    await generalForm.toggleEnabled();
    await generalForm.selectRule(expectedRules[4]);
    await generalForm.selectSeverity(expectedSeverities[1]);

    expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
  });

  describe('timeframe form tests', () => {
    let timeframeForm: RuntimeAlertCreateTimeframeHarness;
    beforeEach(async () => {
      timeframeForm = await componentHarness.getTimeframeFormHarness();
    });

    it('should toggle business days', async () => {
      expect(await timeframeForm.getDaysOptions()).toStrictEqual(Days.getAllDayNames());

      await timeframeForm.toggleBusinessDays();
      expect(await timeframeForm.getSelectedDays()).toStrictEqual(Days.getBusinessDays().join(', '));

      await timeframeForm.toggleBusinessDays();
      expect(await timeframeForm.getSelectedDays()).toStrictEqual('');
    });

    it('should select days', async () => {
      expect(await timeframeForm.getDaysOptions()).toStrictEqual(Days.getAllDayNames());

      await timeframeForm.selectDays(Days.getBusinessDays());
      expect(await timeframeForm.getBusinessDaysToggleValue()).toBeTruthy();

      await timeframeForm.selectDays(['Saturday']);
      expect(await timeframeForm.getBusinessDaysToggleValue()).toBeFalsy();

      await timeframeForm.selectDays(['Saturday']);
      expect(await timeframeForm.getBusinessDaysToggleValue()).toBeTruthy();

      await timeframeForm.selectDays(['Monday']);
      expect(await timeframeForm.getBusinessDaysToggleValue()).toBeFalsy();
    });

    it('should toggle office hours', async () => {
      expect(await timeframeForm.getTimeRange()).toStrictEqual('');

      await timeframeForm.toggleOfficeHours();
      expect(await timeframeForm.getTimeRange()).toStrictEqual('09:00 - 18:00');

      await timeframeForm.toggleOfficeHours();
      expect(await timeframeForm.getTimeRange()).toStrictEqual('');
    });

    it('should set time range', async () => {
      expect(await timeframeForm.getTimeRange()).toStrictEqual('');

      await timeframeForm.setTimeRange('09:00 - 18:00');
      expect(await timeframeForm.getOfficeHoursToggleValue()).toBeTruthy();

      await timeframeForm.setTimeRange('09:00 - 12:00');
      expect(await timeframeForm.getOfficeHoursToggleValue()).toBeFalsy();

      expect(await componentHarness.isSubmitInvalid()).toBeTruthy();
    });

    it('should save alert with timeframe', async () => {
      await fillGeneralForm(1);
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const missingDataForm = await conditionForm.missingDataConditionForm();

      await timeframeForm.toggleBusinessDays();
      await timeframeForm.toggleOfficeHours();
      await missingDataForm.setDurationValue('3000');
      await missingDataForm.selectTimeUnit('Seconds');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();

      await componentHarness.createClick();
      expectAlertPostRequest({
        conditions: [
          {
            duration: 3000,
            timeUnit: 'SECONDS',
            type: 'MISSING_DATA',
          },
        ],
        description: null,
        enabled: true,
        filters: undefined,
        name: 'alert',
        notificationPeriods: [
          {
            beginHour: 32400,
            days: [1, 2, 3, 4, 5],
            endHour: 64800,
            zoneId: 'UTC',
          },
        ],
        notifications: [],
        reference_id: 'apiId',
        reference_type: 'API',
        severity: 'WARNING',
        source: 'REQUEST',
        template: false,
        type: 'MISSING_DATA',
      });
    });
  });

  describe('filters tests', () => {
    const API_METRICS = [
      'Response Time (ms)',
      'Upstream Response Time (ms)',
      'Status Code',
      'Request Content-Length',
      'Response Content-Length',
      'Error Key',
      'Tenant',
      'Application',
      'Plan',
    ];
    const HEALTH_CHECK_METRICS = ['Old Status', 'New Status', 'Endpoint name', 'Response Time (ms)', 'Tenant'];

    it('should display rule selection banner', async () => {
      const filtersForm = await componentHarness.getFiltersFormHarness();
      expect(await filtersForm.isImpactBannerDisplayed()).toBeTruthy();

      await fillGeneralForm(1);
      expect(await filtersForm.isImpactBannerDisplayed()).toBeFalsy();
    });

    it.each`
      ruleIndex | metrics
      ${0}      | ${API_METRICS}
      ${1}      | ${API_METRICS}
      ${2}      | ${API_METRICS}
      ${3}      | ${API_METRICS}
      ${4}      | ${HEALTH_CHECK_METRICS}
    `('should calculate metrics according to selected rule', async ({ ruleIndex, metrics }) => {
      await fillGeneralForm(ruleIndex);

      const filtersForm = await componentHarness.getFiltersFormHarness();
      await filtersForm.addFilter();

      const metricsSimpleCondition = await filtersForm.getMetricsCondition(0);
      expect(await metricsSimpleCondition.getMetricOptions()).toStrictEqual(metrics);
    });

    it('should be able to add and remove metrics conditions', async () => {
      await fillGeneralForm(2);
      const filtersForm = await componentHarness.getFiltersFormHarness();

      expect(await filtersForm.getMetricsConditionsLength()).toStrictEqual(0);

      await filtersForm.addFilter();
      expect(await filtersForm.getMetricsConditionsLength()).toStrictEqual(1);

      await filtersForm.addFilter();
      expect(await filtersForm.getMetricsConditionsLength()).toStrictEqual(2);

      await filtersForm.deleteFilter();
      expect(await filtersForm.getMetricsConditionsLength()).toStrictEqual(1);
    });

    it('should create alert with filters', async () => {
      await fillGeneralForm(4);
      const filtersForm = await componentHarness.getFiltersFormHarness();
      await filtersForm.addFilter();

      const metricsSimpleCondition = await filtersForm.getMetricsCondition(0);
      await metricsSimpleCondition.selectMetric('Old Status');
      await metricsSimpleCondition.selectType('STRING');
      await metricsSimpleCondition.selectOperator('equals to');

      const expectedStatus = ['Down', 'Transitionally down', 'Transitionally up', 'Up'];
      expect(await metricsSimpleCondition.getReferenceOptions()).toStrictEqual(expectedStatus);

      await metricsSimpleCondition.selectMetric('New Status');
      await metricsSimpleCondition.selectType('STRING');
      await metricsSimpleCondition.selectOperator('equals to');
      expect(await metricsSimpleCondition.getReferenceOptions()).toStrictEqual(expectedStatus);

      await metricsSimpleCondition.selectMetric('Endpoint name');
      await metricsSimpleCondition.selectType('STRING');
      await metricsSimpleCondition.selectOperator('starts with');
      await metricsSimpleCondition.setReferenceValue('endpoint-pattern');

      expect(await metricsSimpleCondition.getSelectedMetric()).toStrictEqual('Endpoint name');
      expect(await metricsSimpleCondition.getSelectedType()).toStrictEqual('STRING');
      expect(await metricsSimpleCondition.getSelectedOperator()).toStrictEqual('starts with');
      expect(await metricsSimpleCondition.getReferenceValue()).toStrictEqual('endpoint-pattern');
      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();

      await componentHarness.createClick();

      expectAlertPostRequest({
        conditions: [
          {
            operator: 'NOT_EQUALS',
            projections: null,
            property: 'status.old',
            property2: 'status.new',
            type: 'STRING_COMPARE',
          },
        ],
        description: null,
        enabled: true,
        filters: [
          {
            ignoreCase: true,
            operator: 'STARTS_WITH',
            pattern: 'endpoint-pattern',
            property: 'endpoint.name',
            type: 'STRING',
          },
        ],
        name: 'alert',
        notificationPeriods: null,
        notifications: [],
        reference_id: 'apiId',
        reference_type: 'API',
        severity: 'WARNING',
        source: 'ENDPOINT_HEALTH_CHECK',
        template: false,
        type: 'API_HC_ENDPOINT_STATUS_CHANGED',
      });
    });
  });

  function expectAlertPostRequest(alert: NewAlertTriggerEntity) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/alerts` });
    expect(req.request.body).toBeTruthy();
    expect(req.request.body).toEqual(alert);
    req.flush(alert);
    fixture.detectChanges();
  }
});
