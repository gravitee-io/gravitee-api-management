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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiLoggingComponent } from './api-logging.component';
import { ApiLoggingModule } from './api-logging.module';
import { ApiLoggingHarness } from './api-logging.harness';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { ConsoleSettings } from '../../../entities/consoleSettings';

describe('ApiLogging', () => {
  let httpTestingController: HttpTestingController;

  const settings: ConsoleSettings = {
    metadata: {
      readonly: [],
    },
    logging: {
      maxDurationMillis: 0,
      audit: {
        enabled: false,
        trail: {
          enabled: false,
        },
      },
      user: {
        displayed: false,
      },
      messageSampling: {
        probabilistic: {
          limit: 0.5,
          default: 0.33322,
        },
        count: {
          limit: 55,
          default: 666,
        },
        temporal: {
          limit: 'PT1S',
          default: 'PT1S',
        },
      },
    },
  };

  let fixture: ComponentFixture<ApiLoggingComponent>;
  let componentHarness: ApiLoggingHarness;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatIconTestingModule, ApiLoggingModule],
      providers: [
        {
          provide: 'Constants',
          useValue: CONSTANTS_TESTING,
        },
      ],
    }).compileComponents();
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(ApiLoggingComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiLoggingHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be loading when no setting is provided', async () => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);

    expect(fixture.componentInstance.isLoading).toBeTruthy();
  });

  describe('Max Duration', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        ...settings,
        metadata: {
          readonly: ['logging.default.max.duration'],
        },
      });

      expect(await componentHarness.isMaxDurationFieldDisabled()).toEqual(true);
    });

    it('should save duration settings', async () => {
      expectConsoleSettingsGetRequest(settings);

      expect(await componentHarness.isMaxDurationFieldDisabled()).toEqual(false);
      await componentHarness.setMaxDuration('55');

      await componentHarness.saveSettings();

      expectConsoleSettingsSendRequest({
        logging: {
          ...settings.logging,
          maxDurationMillis: 55,
        },
      });
    });
  });

  describe('Audit', () => {
    it('should disable fields when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        ...settings,
        metadata: {
          readonly: ['logging.audit.enabled', 'logging.audit.trail.enabled'],
        },
      });

      expect(await componentHarness.isAuditEnabledToggleDisabled()).toEqual(true);
      expect(await componentHarness.isAuditTrailEnabledToggleDisabled()).toEqual(true);
    });

    it('should save audit settings', async () => {
      expectConsoleSettingsGetRequest(settings);

      expect(await componentHarness.isAuditEnabledToggleDisabled()).toEqual(false);
      await componentHarness.toggleAuditEnabled();

      expect(await componentHarness.isAuditTrailEnabledToggleDisabled()).toEqual(false);
      await componentHarness.toggleAuditTrailEnabled();

      await componentHarness.saveSettings();

      expectConsoleSettingsSendRequest({
        logging: {
          ...settings.logging,
          audit: {
            enabled: true,
            trail: {
              enabled: true,
            },
          },
        },
      });
    });
  });

  describe('User', () => {
    it('should disable field when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        ...settings,
        metadata: {
          readonly: ['logging.user.displayed'],
        },
      });

      expect(await componentHarness.isUserDisplayedToggleDisabled()).toEqual(true);
    });

    it('should save user settings', async () => {
      expectConsoleSettingsGetRequest(settings);

      expect(await componentHarness.isUserDisplayedToggleDisabled()).toEqual(false);
      await componentHarness.toggleUserDisplayed();

      await componentHarness.saveSettings();

      expectConsoleSettingsSendRequest({
        logging: {
          ...settings.logging,
          user: {
            displayed: true,
          },
        },
      });
    });
  });

  describe('Message Sampling', () => {
    it('should disable fields when setting is readonly', async () => {
      expectConsoleSettingsGetRequest({
        ...settings,
        metadata: {
          readonly: [
            'logging.messageSampling.count.default',
            'logging.messageSampling.count.limit',
            'logging.messageSampling.probabilistic.default',
            'logging.messageSampling.probabilistic.limit',
            'logging.messageSampling.temporal.default',
            'logging.messageSampling.temporal.limit',
          ],
        },
      });

      expect(await componentHarness.countDefaultIsDisabled()).toBe(true);
      expect(await componentHarness.countLimitIsDisabled()).toBe(true);

      expect(await componentHarness.probabilisticDefaultIsDisabled()).toBe(true);
      expect(await componentHarness.probabilisticLimitIsDisabled()).toBe(true);

      expect(await componentHarness.temporalDefaultIsDisabled()).toBe(true);
      expect(await componentHarness.temporalLimitIsDisabled()).toBe(true);
    });

    it('should save message sampling settings', async () => {
      expectConsoleSettingsGetRequest(settings);

      await componentHarness.setCountDefault('500');
      await componentHarness.setCountLimit('60');

      await componentHarness.setProbabilisticDefault('0.25');
      await componentHarness.setProbabilisticLimit('0.98');

      await componentHarness.setTemporalDefault('PT20S');
      await componentHarness.setTemporalLimit('PT15S');

      expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      await componentHarness.saveSettings();

      expectConsoleSettingsSendRequest({
        logging: {
          ...settings.logging,
          messageSampling: {
            count: {
              default: 500,
              limit: 60,
            },
            probabilistic: {
              default: 0.25,
              limit: 0.98,
            },
            temporal: {
              default: 'PT20S',
              limit: 'PT15S',
            },
          },
        },
      });
    });

    describe('Message Sampling - Count - Validation', () => {
      it('should have minimal value greater than 1', async () => {
        expectConsoleSettingsGetRequest(settings);

        // Check minimum value is 1
        await componentHarness.setCountDefault('0');
        await componentHarness.setCountLimit('0');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default value should be at least 1']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Limit value should be at least 1']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setCountDefault('60');
        await componentHarness.setCountLimit('60');
        expect(await componentHarness.countDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.countLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have non empty values', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setCountDefault(null);
        await componentHarness.setCountLimit(null);
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        await componentHarness.setCountDefault('');
        await componentHarness.setCountLimit('');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setCountDefault('60');
        await componentHarness.setCountLimit('60');
        expect(await componentHarness.countDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.countLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have integer values', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setCountDefault('12.2');
        await componentHarness.setCountLimit('2.5');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default value should be an integer']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Limit value should be an integer']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setCountDefault('60');
        await componentHarness.setCountLimit('60');
        expect(await componentHarness.countDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.countLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have default greater than limit', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setCountDefault('10');
        await componentHarness.setCountLimit('20');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Try to change only limit value
        await componentHarness.setCountLimit('15');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        // Try to change only default value
        await componentHarness.setCountDefault('12');
        expect(await componentHarness.countDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.countLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        // Choose valid values
        await componentHarness.setCountDefault('20');
        expect(await componentHarness.countDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.countLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });
    });

    describe('Message Sampling - Probabilistic - Validation', () => {
      it('should have minimal value greater than 0.01', async () => {
        expectConsoleSettingsGetRequest(settings);

        // Check minimum value is 0.01
        await componentHarness.setProbabilisticDefault('0');
        await componentHarness.setProbabilisticLimit('0');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default value should be at least 0.01']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Limit value should be at least 0.01']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setProbabilisticDefault('0.01');
        await componentHarness.setProbabilisticLimit('0.01');
        expect(await componentHarness.probabilisticDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.probabilisticLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have minimal value lower than 1', async () => {
        expectConsoleSettingsGetRequest(settings);

        // Check maximum value is 1
        await componentHarness.setProbabilisticDefault('10');
        await componentHarness.setProbabilisticLimit('20');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default value should not be greater than 1']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Limit value should not be greater than 1']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setProbabilisticDefault('0.01');
        await componentHarness.setProbabilisticLimit('0.01');
        expect(await componentHarness.probabilisticDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.probabilisticLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have non empty values', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setProbabilisticDefault(null);
        await componentHarness.setProbabilisticLimit(null);
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        await componentHarness.setProbabilisticDefault('');
        await componentHarness.setProbabilisticLimit('');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setProbabilisticDefault('0.01');
        await componentHarness.setProbabilisticLimit('0.01');
        expect(await componentHarness.probabilisticDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.probabilisticLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have default lower than limit', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setProbabilisticDefault('0.8');
        await componentHarness.setProbabilisticLimit('0.2');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default should be lower than Limit']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Default should be lower than Limit']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Try to change only limit value
        await componentHarness.setProbabilisticLimit('0.3');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default should be lower than Limit']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Default should be lower than Limit']);

        // Try to change only default value
        await componentHarness.setProbabilisticDefault('0.5');
        expect(await componentHarness.probabilisticDefaultTextErrors()).toEqual(['Default should be lower than Limit']);
        expect(await componentHarness.probabilisticLimitTextErrors()).toEqual(['Default should be lower than Limit']);

        // Choose valid values
        await componentHarness.setProbabilisticDefault('0.2');
        expect(await componentHarness.probabilisticDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.probabilisticLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });
    });

    describe('Message Sampling - Temporal - Validation', () => {
      it('should have non empty values', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setTemporalDefault(null);
        await componentHarness.setTemporalLimit(null);
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        await componentHarness.setTemporalDefault('');
        await componentHarness.setTemporalLimit('');
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default value is required']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Limit value is required']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setTemporalDefault('PT2S');
        await componentHarness.setTemporalLimit('PT1S');
        expect(await componentHarness.temporalDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.temporalLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have bea valid ISO 8601 duration', async () => {
        expectConsoleSettingsGetRequest(settings);

        // Check minimum value is 0.01
        await componentHarness.setTemporalDefault('PT0Seconds');
        await componentHarness.setTemporalLimit('PT0Seconds');
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default value should conform to ISO-8601 duration format']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Limit value should conform to ISO-8601 duration format']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Choose valid values
        await componentHarness.setTemporalDefault('PT1S');
        await componentHarness.setTemporalLimit('PT1S');
        expect(await componentHarness.temporalDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.temporalLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });

      it('should have default greater than limit', async () => {
        expectConsoleSettingsGetRequest(settings);

        await componentHarness.setTemporalDefault('PT10S');
        await componentHarness.setTemporalLimit('PT20S');
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

        // Try to change only limit value
        await componentHarness.setTemporalLimit('PT18S');
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        // Try to change only default value
        await componentHarness.setTemporalDefault('PT13S');
        expect(await componentHarness.temporalDefaultTextErrors()).toEqual(['Default should be greater than Limit']);
        expect(await componentHarness.temporalLimitTextErrors()).toEqual(['Default should be greater than Limit']);

        // Choose valid values
        await componentHarness.setTemporalDefault('PT1M');
        expect(await componentHarness.temporalDefaultHasErrors()).toBeFalsy();
        expect(await componentHarness.temporalLimitHasErrors()).toBeFalsy();

        expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
      });
    });
  });

  function expectConsoleSettingsSendRequest(consoleSettingsPayload: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    expect(req.request.body).toMatchObject(consoleSettingsPayload);
    expect(req.request.method).toEqual('POST');
  }

  function expectConsoleSettingsGetRequest(consoleSettingsResponse: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    req.flush(consoleSettingsResponse);
    expect(req.request.method).toEqual('GET');
  }
});
