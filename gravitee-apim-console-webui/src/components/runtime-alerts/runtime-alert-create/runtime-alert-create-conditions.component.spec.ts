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
import { MetricsSimpleConditionHarness } from './components/components/metrics-simple-condition/metrics-simple-condition.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeTenant } from '../../../entities/tenant/tenant.fixture';
import { NewAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';
import { ThresholdRangeCondition } from '../../../entities/alerts/conditions';

// toDo fix when FE implementation is finished.
xdescribe('RuntimeAlertCreateComponent condition tests', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envId';
  const expectedAlert: NewAlertTriggerEntity = {
    conditions: [],
    description: null,
    enabled: true,
    name: 'alert',
    notificationPeriods: null,
    notifications: [],
    reference_id: 'apiId',
    reference_type: 'API',
    severity: 'WARNING',
    source: 'REQUEST',
    template: false,
    type: 'METRICS_SIMPLE_CONDITION',
  };

  let fixture: ComponentFixture<RuntimeAlertCreateComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: RuntimeAlertCreateHarness;

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

  it('should display rule selection banner', async () => {
    const conditionForm = await componentHarness.getConditionsFormHarness();
    expect(await conditionForm.isImpactBannerDisplayed()).toBeTruthy();

    await fillGeneralForm(1);

    expect(await conditionForm.isImpactBannerDisplayed()).toBeFalsy();
  });

  describe('with missing data condition', () => {
    beforeEach(async () => await fillGeneralForm(1));

    it('should add time duration condition', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const missingDataForm = await conditionForm.missingDataConditionForm();

      const expectedTimeUnits = ['Seconds', 'Minutes', 'Hours'];
      expect(await missingDataForm.getTimeUnitOptions()).toStrictEqual(expectedTimeUnits);

      await missingDataForm.setDurationValue('3000');
      await missingDataForm.selectTimeUnit('Seconds');

      expect(await missingDataForm.getDurationValue()).toStrictEqual('3000');
      expect(await missingDataForm.getSelectedTimeUnit()).toStrictEqual('Seconds');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [{ duration: 3000, timeUnit: 'SECONDS', type: 'MISSING_DATA' }],
        source: 'REQUEST',
        type: 'MISSING_DATA',
      });
    });
  });

  describe('with request metrics simple condition', () => {
    let metricSimpleConditionForm: MetricsSimpleConditionHarness;
    beforeEach(async () => {
      await fillGeneralForm(0);

      const conditionForm = await componentHarness.getConditionsFormHarness();
      metricSimpleConditionForm = await conditionForm.metricsSimpleConditionForm();
      expect(await metricSimpleConditionForm.getMetricOptions()).toStrictEqual([
        'Response Time (ms)',
        'Upstream Response Time (ms)',
        'Status Code',
        'Request Content-Length',
        'Response Content-Length',
        'Error Key',
        'Tenant',
        'Application',
        'Plan',
      ]);
      expect(await metricSimpleConditionForm.isTypeSelectDisabled()).toBeTruthy();
    });

    it('should add threshold condition', async () => {
      await metricSimpleConditionForm.selectMetric('Status Code');
      expect(await metricSimpleConditionForm.isTypeSelectDisabled()).toBeFalsy();
      expect(await metricSimpleConditionForm.getTypeOptions()).toStrictEqual(['THRESHOLD', 'THRESHOLD_RANGE']);

      await metricSimpleConditionForm.selectType('THRESHOLD');
      expect(await metricSimpleConditionForm.getOperatorOptions()).toStrictEqual([
        'less than',
        'less than or equals to',
        'greater than or equals to',
        'greater than',
      ]);

      await metricSimpleConditionForm.selectOperator('less than or equals to');
      await metricSimpleConditionForm.setThresholdValue('42');

      expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Status Code');
      expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('THRESHOLD');
      expect(await metricSimpleConditionForm.getSelectedOperator()).toStrictEqual('less than or equals to');
      expect(await metricSimpleConditionForm.getThresholdValue()).toStrictEqual('42');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [{ operator: 'LTE', property: 'response.status', threshold: 42, type: 'THRESHOLD' }],
        source: 'REQUEST',
        type: 'METRICS_SIMPLE_CONDITION',
      });
    });

    it('should add threshold range condition', async () => {
      await metricSimpleConditionForm.selectMetric('Status Code');
      expect(await metricSimpleConditionForm.isTypeSelectDisabled()).toBeFalsy();
      expect(await metricSimpleConditionForm.getTypeOptions()).toStrictEqual(['THRESHOLD', 'THRESHOLD_RANGE']);

      await metricSimpleConditionForm.selectType('THRESHOLD_RANGE');

      await metricSimpleConditionForm.setLowThresholdValue('50');
      await metricSimpleConditionForm.setHighThresholdValue('42');
      expect(await metricSimpleConditionForm.isHighThresholdInvalid()).toBeTruthy();

      await metricSimpleConditionForm.setHighThresholdValue('60');

      expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Status Code');
      expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('THRESHOLD_RANGE');
      expect(await metricSimpleConditionForm.getLowThresholdValue()).toStrictEqual('50');
      expect(await metricSimpleConditionForm.isHighThresholdInvalid()).toBeFalsy();
      expect(await metricSimpleConditionForm.getHighThresholdValue()).toStrictEqual('60');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            operatorHigh: 'INCLUSIVE',
            operatorLow: 'INCLUSIVE',
            property: 'response.status',
            thresholdHigh: 60,
            thresholdLow: 50,
            type: 'THRESHOLD_RANGE',
          },
        ],
        source: 'REQUEST',
        type: 'METRICS_SIMPLE_CONDITION',
      });
    });

    it('should add compare condition', async () => {
      await metricSimpleConditionForm.selectMetric('Response Time (ms)');
      expect(await metricSimpleConditionForm.isTypeSelectDisabled()).toBeFalsy();
      expect(await metricSimpleConditionForm.getTypeOptions()).toStrictEqual(['THRESHOLD', 'THRESHOLD_RANGE', 'COMPARE']);

      await metricSimpleConditionForm.selectType('COMPARE');
      await metricSimpleConditionForm.setMultiplierValue('42');
      expect(await metricSimpleConditionForm.getPropertyOptions()).toStrictEqual([
        'Upstream Response Time (ms)',
        'Request Content-Length',
        'Response Content-Length',
      ]);
      expect(await metricSimpleConditionForm.getOperatorOptions()).toStrictEqual([
        'less than',
        'less than or equals to',
        'greater than or equals to',
        'greater than',
      ]);

      await metricSimpleConditionForm.selectProperty('Request Content-Length');
      await metricSimpleConditionForm.selectOperator('greater than');

      expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Response Time (ms)');
      expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('COMPARE');
      expect(await metricSimpleConditionForm.getSelectedOperator()).toStrictEqual('greater than');
      expect(await metricSimpleConditionForm.getMultiplierValue()).toStrictEqual('42');
      expect(await metricSimpleConditionForm.getSelectedProperty()).toStrictEqual('Request Content-Length');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          { multiplier: 42, operator: 'GT', property: 'response.response_time', property2: 'request.content_length', type: 'COMPARE' },
        ],
        source: 'REQUEST',
        type: 'METRICS_SIMPLE_CONDITION',
      });
    });

    describe('should add string condition', () => {
      beforeEach(async () => {
        await metricSimpleConditionForm.selectMetric('Tenant');
        expect(await metricSimpleConditionForm.isTypeSelectDisabled()).toBeFalsy();
        expect(await metricSimpleConditionForm.getTypeOptions()).toStrictEqual(['STRING']);
        await metricSimpleConditionForm.selectType('STRING');
        expect(await metricSimpleConditionForm.getOperatorOptions()).toStrictEqual([
          'equals to',
          'not equals to',
          'starts with',
          'ends with',
          'contains',
          'matches',
        ]);
      });

      it(' with fixed value', async () => {
        await metricSimpleConditionForm.selectOperator('not equals to');

        const tenant1 = fakeTenant({ id: '1', name: 'tenant-1' });
        const tenant2 = fakeTenant({ id: '2', name: 'tenant-2' });
        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`,
            method: 'GET',
          })
          .flush([tenant1, tenant2]);
        fixture.detectChanges();
        expect(await metricSimpleConditionForm.getReferenceOptions()).toStrictEqual(['tenant-1', 'tenant-2']);

        await metricSimpleConditionForm.selectReference('tenant-2');

        expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Tenant');
        expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('STRING');
        expect(await metricSimpleConditionForm.getSelectedOperator()).toStrictEqual('not equals to');
        expect(await metricSimpleConditionForm.getSelectedReference()).toStrictEqual('tenant-2');

        expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
        await componentHarness.createClick();
        expectAlertPostRequest({
          ...expectedAlert,
          conditions: [{ ignoreCase: true, operator: 'NOT_EQUALS', pattern: '2', property: 'tenant', type: 'STRING' }],
          source: 'REQUEST',
          type: 'METRICS_SIMPLE_CONDITION',
        });
      });

      it('with pattern value', async () => {
        expect(await metricSimpleConditionForm.selectOperator('ends with'));

        await metricSimpleConditionForm.setReferenceValue('.*tenant.*');

        expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Tenant');
        expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('STRING');
        expect(await metricSimpleConditionForm.getSelectedOperator()).toStrictEqual('ends with');
        expect(await metricSimpleConditionForm.getReferenceValue()).toStrictEqual('.*tenant.*');

        expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
        await componentHarness.createClick();
        expectAlertPostRequest({
          ...expectedAlert,
          conditions: [{ ignoreCase: true, operator: 'ENDS_WITH', pattern: '.*tenant.*', property: 'tenant', type: 'STRING' }],
          source: 'REQUEST',
          type: 'METRICS_SIMPLE_CONDITION',
        });
      });
    });
  });

  describe('with request metrics aggregation condition', () => {
    beforeEach(async () => await fillGeneralForm(2));

    it('should add condition', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const requestMetricsAggregationForm = await conditionForm.requestMetricsAggregationConditionForm();

      expect(await requestMetricsAggregationForm.getFunctionOptions()).toStrictEqual([
        'count',
        'average',
        'min',
        'max',
        '50th percentile',
        '90th percentile',
        '95th percentile',
        '99th percentile',
      ]);
      expect(await requestMetricsAggregationForm.getMetricOptions()).toStrictEqual([
        'Response Time (ms)',
        'Upstream Response Time (ms)',
        'Request Content-Length',
        'Response Content-Length',
      ]);

      await requestMetricsAggregationForm.selectFunction('50th percentile');
      await requestMetricsAggregationForm.selectMetric('Request Content-Length');

      const thresholdSubform = await requestMetricsAggregationForm.getThresholdHarness();
      expect(await thresholdSubform.getOperatorOptions()).toStrictEqual([
        'less than',
        'less than or equals to',
        'greater than or equals to',
        'greater than',
      ]);

      await thresholdSubform.selectOperator('greater than');
      await thresholdSubform.setThresholdValue('42');

      const durationSubform = await requestMetricsAggregationForm.durationHarness();
      expect(await durationSubform.getTimeUnitOptions()).toStrictEqual(['Seconds', 'Minutes', 'Hours']);

      await durationSubform.setDurationValue('24');
      await durationSubform.selectTimeUnit('Hours');

      expect(await requestMetricsAggregationForm.getSelectedFunction()).toStrictEqual('50th percentile');
      expect(await requestMetricsAggregationForm.getSelectedMetric()).toStrictEqual('Request Content-Length');
      expect(await thresholdSubform.getSelectedOperator()).toStrictEqual('greater than');
      expect(await thresholdSubform.getThresholdValue()).toStrictEqual('42');
      expect(await durationSubform.getDurationValue()).toStrictEqual('24');
      expect(await durationSubform.getSelectedTimeUnit()).toStrictEqual('Hours');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            duration: 24,
            function: 'P50',
            operator: 'GT',
            projections: null,
            property: 'request.content_length',
            threshold: 42,
            timeUnit: 'HOURS',
            type: 'AGGREGATION',
          },
        ],
        source: 'REQUEST',
        type: 'METRICS_AGGREGATION',
      });
    });

    it('should add condition with aggregation', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const requestMetricsAggregationForm = await conditionForm.requestMetricsAggregationConditionForm();
      await requestMetricsAggregationForm.selectFunction('50th percentile');
      await requestMetricsAggregationForm.selectMetric('Request Content-Length');

      const thresholdSubform = await requestMetricsAggregationForm.getThresholdHarness();
      await thresholdSubform.selectOperator('greater than');
      await thresholdSubform.setThresholdValue('42');

      const durationSubform = await requestMetricsAggregationForm.durationHarness();
      await durationSubform.setDurationValue('24');
      await durationSubform.selectTimeUnit('Hours');

      const aggregationSubform = await requestMetricsAggregationForm.aggregationForm();
      await aggregationSubform.accordionClick();

      expect(await aggregationSubform.getPropertyOptions()).toStrictEqual([
        'None',
        'Status Code',
        'Error Key',
        'Tenant',
        'Application',
        'Plan',
      ]);
      await aggregationSubform.selectProperty('Application');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('Application');

      await aggregationSubform.selectProperty('None');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('');

      await aggregationSubform.selectProperty('Plan');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            duration: 24,
            function: 'P50',
            operator: 'GT',
            projections: [
              {
                property: 'plan',
                type: 'PROPERTY',
              },
            ],
            property: 'request.content_length',
            threshold: 42,
            timeUnit: 'HOURS',
            type: 'AGGREGATION',
          },
        ],
        source: 'REQUEST',
        type: 'METRICS_AGGREGATION',
      });
    });
  });

  describe('with request metrics rate condition', () => {
    beforeEach(async () => await fillGeneralForm(3));

    it('should fill rate condition', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const requestMetricsRateConditionForm = await conditionForm.requestMetricsRateConditionForm();

      const metricsSubform = await requestMetricsRateConditionForm.metricsSimpleConditionForm();
      await metricsSubform.selectMetric('Response Content-Length');
      await metricsSubform.selectType('THRESHOLD_RANGE');
      await metricsSubform.setLowThresholdValue('1000');
      await metricsSubform.setHighThresholdValue('2000');

      const thresholdSubform = await requestMetricsRateConditionForm.getThresholdForm();
      await thresholdSubform.setThresholdValue('1000');
      expect(await thresholdSubform.isThresholdInvalid()).toBeTruthy();

      await thresholdSubform.selectOperator('less than');
      await thresholdSubform.setThresholdValue('50');
      expect(await thresholdSubform.isThresholdInvalid()).toBeFalsy();

      const durationSubform = await requestMetricsRateConditionForm.durationForm();
      await durationSubform.setDurationValue('1');
      await durationSubform.selectTimeUnit('Minutes');

      expect(await metricsSubform.getSelectedMetric()).toStrictEqual('Response Content-Length');
      expect(await metricsSubform.getSelectedType()).toStrictEqual('THRESHOLD_RANGE');
      expect(await metricsSubform.getLowThresholdValue()).toStrictEqual('1000');
      expect(await metricsSubform.getHighThresholdValue()).toStrictEqual('2000');
      expect(await thresholdSubform.getSelectedOperator()).toStrictEqual('less than');
      expect(await thresholdSubform.getThresholdValue()).toStrictEqual('50');
      expect(await durationSubform.getDurationValue()).toStrictEqual('1');
      expect(await durationSubform.getSelectedTimeUnit()).toStrictEqual('Minutes');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();

      const expectedComparison: ThresholdRangeCondition = {
        operatorHigh: 'INCLUSIVE',
        operatorLow: 'INCLUSIVE',
        property: 'response.content_length',
        thresholdHigh: 2000,
        thresholdLow: 1000,
        type: 'THRESHOLD_RANGE',
      };
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            comparison: { ...expectedComparison },
            duration: 1,
            operator: 'LT',
            projections: null,
            threshold: 50,
            timeUnit: 'MINUTES',
            type: 'RATE',
          },
        ],
        source: 'REQUEST',
        type: 'METRICS_RATE',
      });
    });

    it('should fill rate condition with aggregation', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const requestMetricsRateConditionForm = await conditionForm.requestMetricsRateConditionForm();

      const metricsSubform = await requestMetricsRateConditionForm.metricsSimpleConditionForm();
      await metricsSubform.selectMetric('Response Content-Length');
      await metricsSubform.selectType('THRESHOLD_RANGE');
      await metricsSubform.setLowThresholdValue('1000');
      await metricsSubform.setHighThresholdValue('2000');

      const thresholdSubform = await requestMetricsRateConditionForm.getThresholdForm();
      await thresholdSubform.selectOperator('less than');
      await thresholdSubform.setThresholdValue('50');

      const durationSubform = await requestMetricsRateConditionForm.durationForm();
      await durationSubform.setDurationValue('1');
      await durationSubform.selectTimeUnit('Minutes');

      const aggregationSubform = await requestMetricsRateConditionForm.aggregationForm();
      await aggregationSubform.accordionClick();
      expect(await aggregationSubform.getPropertyOptions()).toStrictEqual([
        'None',
        'Status Code',
        'Error Key',
        'Tenant',
        'Application',
        'Plan',
      ]);
      await aggregationSubform.selectProperty('Tenant');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('Tenant');

      await aggregationSubform.selectProperty('None');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('');

      await aggregationSubform.selectProperty('Error Key');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();

      const expectedComparison: ThresholdRangeCondition = {
        operatorHigh: 'INCLUSIVE',
        operatorLow: 'INCLUSIVE',
        property: 'response.content_length',
        thresholdHigh: 2000,
        thresholdLow: 1000,
        type: 'THRESHOLD_RANGE',
      };
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            comparison: { ...expectedComparison },
            duration: 1,
            operator: 'LT',
            projections: [
              {
                property: 'error.key',
                type: 'PROPERTY',
              },
            ],
            threshold: 50,
            timeUnit: 'MINUTES',
            type: 'RATE',
          },
        ],
        source: 'REQUEST',
        type: 'METRICS_RATE',
      });
    });
  });

  describe('with endpoint health check condition', () => {
    beforeEach(async () => await fillGeneralForm(4));

    it('should add property aggregation', async () => {
      const conditionForm = await componentHarness.getConditionsFormHarness();
      const endpointHealthCheckConditionForm = await conditionForm.endpointHealthCheckConditionForm();
      const aggregationSubform = await endpointHealthCheckConditionForm.getAggregationForm();

      await aggregationSubform.accordionClick();
      expect(await aggregationSubform.getPropertyOptions()).toStrictEqual(['None', 'Endpoint name']);

      await aggregationSubform.selectProperty('Endpoint name');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('Endpoint name');

      await aggregationSubform.selectProperty('None');
      expect(await aggregationSubform.getSelectedProperty()).toStrictEqual('');

      await aggregationSubform.selectProperty('Endpoint name');

      expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
      await componentHarness.createClick();
      expectAlertPostRequest({
        ...expectedAlert,
        conditions: [
          {
            operator: 'NOT_EQUALS',
            projections: [
              {
                property: 'endpoint.name',
                type: 'PROPERTY',
              },
            ],
            property: 'status.old',
            property2: 'status.new',
            type: 'STRING_COMPARE',
          },
        ],
        source: 'ENDPOINT_HEALTH_CHECK',
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
