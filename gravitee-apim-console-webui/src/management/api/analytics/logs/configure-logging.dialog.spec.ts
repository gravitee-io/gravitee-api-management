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

import * as moment from 'moment';

import { Condition, ConditionType, ConfigureLoggingDialogController } from './configure-logging.dialog';

import { setupAngularJsTesting } from '../../../../../jest.setup.js';

setupAngularJsTesting();

describe('LogComponent', () => {
  let $mdDialog: angular.material.IDialogService;
  let configureLoggingDialog: ConfigureLoggingDialogController;

  beforeEach(inject((_$controller_, _$mdDialog_) => {
    $mdDialog = _$mdDialog_;
    configureLoggingDialog = _$controller_(
      'configureLoggingDialogController',
      {
        $mdDialog: _$mdDialog_,
        plans: [],
        subscribers: [],
      },
      {},
    );
  }));

  describe('addCondition', () => {
    it('should add a condition', () => {
      configureLoggingDialog.selectedType = 'plan';
      configureLoggingDialog.addCondition();

      expect(configureLoggingDialog.conditions).toEqual([
        new Condition(new ConditionType('Plan', 'plan', '#context.attributes.plan'), '==', ''),
      ]);
    });

    it('should clean selectedType', () => {
      configureLoggingDialog.selectedType = 'unknown';
      configureLoggingDialog.addCondition();

      expect(configureLoggingDialog.selectedType).toBe(null);
    });
  });

  describe('removeCondition', () => {
    it('should remove a condition', () => {
      configureLoggingDialog.conditions.push(new Condition(new ConditionType('1', '1', ''), '', ''));
      configureLoggingDialog.conditions.push(new Condition(new ConditionType('2', '2', ''), '', ''));
      configureLoggingDialog.conditions.push(new Condition(new ConditionType('3', '3', ''), '', ''));
      configureLoggingDialog.removeCondition(1);

      expect(configureLoggingDialog.conditions).toHaveLength(2);
      expect(configureLoggingDialog.conditions[0].type.id).toBe('1');
      expect(configureLoggingDialog.conditions[1].type.id).toBe('3');
    });
  });

  describe('save', () => {
    it('should close the dialog with correct condition', () => {
      const hideSpy = spyOn($mdDialog, 'hide');

      configureLoggingDialog.selectedType = 'plan';
      configureLoggingDialog.addCondition();
      configureLoggingDialog.conditions[0].value = '12';
      configureLoggingDialog.save();

      expect(hideSpy).toHaveBeenCalledWith("#context.attributes.plan == '12'");
    });
  });

  describe('hide', () => {
    it('should close the dialog', () => {
      const hideSpy = spyOn($mdDialog, 'hide');

      configureLoggingDialog.hide();

      expect(hideSpy).toHaveBeenCalled();
    });
  });
});

describe('Condition', () => {
  const Plan = new ConditionType('Plan', 'plan', '#context.attributes.plan');
  const RequestHeader = new ConditionType('Request header', 'request-header', '#request.headers');
  const RequestParam = new ConditionType('Request query-parameter', 'request-param', '#request.params');
  const Duration = new ConditionType('Duration', 'logging-duration', '#request.timestamp');
  const EndDate = new ConditionType('End date', 'logging-end-date', '#request.timestamp');

  describe('toCondition', () => {
    it('should convert date for duration condition', () => {
      const condition = new Condition(Duration, '', '');
      condition.param1 = 2;
      condition.param2 = 'day';
      expect(condition.toCondition()).toMatch(/#request.timestamp <= \d+l/);
    });

    it('should convert date for end date condition', () => {
      const condition = new Condition(EndDate, '', '');
      condition.param1 = moment('2018-01-01T00:00:00.000Z');
      expect(condition.toCondition()).toEqual('#request.timestamp <= 1514764800000l');
    });

    it('should use params for request header condition', () => {
      const condition = new Condition(RequestHeader, '==', '42');
      condition.param1 = 'param1';
      expect(condition.toCondition()).toEqual("#request.headers['param1'] != null && #request.headers['param1'][0] == '42'");
    });

    it('should use params for request param condition', () => {
      const condition = new Condition(RequestParam, '==', '42');
      condition.param1 = 'param1';
      expect(condition.toCondition()).toEqual("#request.params['param1'] != null && #request.params['param1'][0] == '42'");
    });

    it('should use params for plan condition', () => {
      const condition = new Condition(Plan, '==', '42');
      condition.param1 = 'param1';
      expect(condition.toCondition()).toEqual("#context.attributes.plan == '42'");
    });
  });

  describe('isValid', () => {
    it('should validate duration condition', () => {
      const condition = new Condition(Duration, '', '');
      condition.param1 = null;
      condition.param2 = 'day';
      expect(condition.isValid()).toBeFalsy();

      condition.param1 = 2;
      condition.param2 = null;
      expect(condition.isValid()).toBeFalsy();

      condition.param1 = 2;
      condition.param2 = 'day';
      expect(condition.isValid()).toBeTruthy();
    });

    it('should validate end date condition', () => {
      const condition = new Condition(EndDate, '', '');

      condition.param1 = null;
      expect(condition.isValid()).toBeFalsy();

      condition.param1 = moment('2018-01-01T00:00:00.000Z');
      expect(condition.isValid()).toBeTruthy();
    });

    it('should validate request header condition', () => {
      const condition = new Condition(RequestHeader, '==', '42');

      condition.param1 = null;
      expect(condition.isValid()).toBeFalsy();

      condition.param1 = 'param1';
      expect(condition.isValid()).toBeTruthy();
    });

    it('should validate request param condition', () => {
      const condition = new Condition(RequestParam, '==', '42');
      condition.param1 = null;
      expect(condition.isValid()).toBeFalsy();

      condition.param1 = 'param1';
      condition.value = '';
      expect(condition.isValid()).toBeFalsy();

      condition.value = '42';
      expect(condition.isValid()).toBeTruthy();
    });

    it('should validate plan condition', () => {
      const condition = new Condition(Plan, '==', '42');

      condition.value = '';
      expect(condition.isValid()).toBeFalsy();

      condition.value = '42';
      expect(condition.isValid()).toBeTruthy();
    });
  });
});
