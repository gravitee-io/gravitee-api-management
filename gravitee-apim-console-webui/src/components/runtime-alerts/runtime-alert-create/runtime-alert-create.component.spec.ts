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

import { Scope } from '../../../entities/alert';
import { GioTestingModule } from '../../../shared/testing';
import { Days } from '../../../entities/alerts/period';

describe('RuntimeAlertCreateComponent', () => {
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
              data: { referenceType: Scope.API },
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

    // TODO test save bar when save is implemented in next commits
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
    });

    // TODO test save bar when save is implemented in next commits
  });
});
