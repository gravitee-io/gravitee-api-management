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
import { MetricsSimpleConditionHarness } from './components/runtime-alert-create-conditions/components/metrics-simple-condition/metrics-simple-condition.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeTenant } from '../../../entities/tenant/tenant.fixture';

describe('RuntimeAlertCreateComponent condition tests', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envId';
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

  describe('missing data condition', () => {
    beforeEach(async () => {
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
      await generalForm.selectRule(expectedRules[1]);
    });

    it('should add time duration condition', async () => {
      const conditionForm = await componentHarness.getConditionFormHarness();
      const missingDataForm = await conditionForm.missingDataConditionForm();

      const expectedTimeUnits = ['Seconds', 'Minutes', 'Hours'];
      expect(await missingDataForm.getTimeUnitOptions()).toStrictEqual(expectedTimeUnits);

      await missingDataForm.setDurationValue('3000');
      await missingDataForm.selectTimeUnit('Seconds');

      expect(await missingDataForm.getDurationValue()).toStrictEqual('3000');
      expect(await missingDataForm.getSelectedTimeUnit()).toStrictEqual('Seconds');
      // TODO test save bar when save is implemented in next commits
    });
  });

  describe('request metrics simple condition', () => {
    let metricSimpleConditionForm: MetricsSimpleConditionHarness;
    beforeEach(async () => {
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
      await generalForm.selectRule(expectedRules[0]);

      const conditionForm = await componentHarness.getConditionFormHarness();
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

      // TODO test save bar when save is implemented in next commits
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

      // TODO test save bar when save is implemented in next commits
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

      // TODO test save bar when save is implemented in next commits
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
        // TODO test save bar when save is implemented in next commits
      });

      it('with pattern value', async () => {
        expect(await metricSimpleConditionForm.selectOperator('ends with'));

        await metricSimpleConditionForm.setReferenceValue('.*tenant.*');

        expect(await metricSimpleConditionForm.getSelectedMetric()).toStrictEqual('Tenant');
        expect(await metricSimpleConditionForm.getSelectedType()).toStrictEqual('STRING');
        expect(await metricSimpleConditionForm.getSelectedOperator()).toStrictEqual('ends with');
        expect(await metricSimpleConditionForm.getReferenceValue()).toStrictEqual('.*tenant.*');
        // TODO test save bar when save is implemented in next commits
      });
    });
  });

  describe('request metrics aggregation', () => {
    beforeEach(async () => {
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
      await generalForm.selectRule(expectedRules[2]);
    });

    it('should add time duration condition', async () => {
      const conditionForm = await componentHarness.getConditionFormHarness();
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
      // TODO test save bar when save is implemented in next commits
    });
  });
});
