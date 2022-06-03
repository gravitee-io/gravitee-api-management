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
