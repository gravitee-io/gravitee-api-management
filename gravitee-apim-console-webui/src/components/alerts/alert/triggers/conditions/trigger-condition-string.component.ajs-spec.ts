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
import { IComponentControllerService } from 'angular';

import { setupAngularJsTesting } from '../../../../../../old-jest.setup';
import { Metrics, Scope } from '../../../../../entities/alert';

setupAngularJsTesting();

describe('AlertTriggerConditionStringComponent', () => {
  let $componentController: IComponentControllerService;
  let alertTriggerConditionStringComponent: any;

  beforeEach(inject(_$componentController_ => {
    $componentController = _$componentController_;
    alertTriggerConditionStringComponent = $componentController('gvAlertTriggerConditionString', null, {});
  }));

  describe('onInit', () => {
    const metric: any = {} as Metrics;
    let metricLoaderSpy: jest.SpyInstance;

    beforeEach(() => {
      metric.key = 'test-property';
      metric.loader = () => {
        return 'test';
      };
      metricLoaderSpy = jest.spyOn(metric, 'loader');

      alertTriggerConditionStringComponent.condition = { property: 'test-property' };
      alertTriggerConditionStringComponent.metrics = [metric];
    });

    it('should call loader with environment type', () => {
      alertTriggerConditionStringComponent.referenceType = Scope.ENVIRONMENT;

      alertTriggerConditionStringComponent.$onInit();

      expect(metricLoaderSpy).toHaveBeenCalledWith(Scope.ENVIRONMENT, undefined, expect.anything());
    });

    it('should call loader with API type', () => {
      alertTriggerConditionStringComponent.referenceType = Scope.API;
      alertTriggerConditionStringComponent.referenceId = 'test-api-id';

      alertTriggerConditionStringComponent.$onInit();

      expect(metricLoaderSpy).toHaveBeenCalledWith(Scope.API, 'test-api-id', expect.anything());
    });

    it('should call loader with APPLICATION type', () => {
      alertTriggerConditionStringComponent.referenceType = Scope.APPLICATION;
      alertTriggerConditionStringComponent.referenceId = 'test-application-id';

      alertTriggerConditionStringComponent.$onInit();

      expect(metricLoaderSpy).toHaveBeenCalledWith(Scope.APPLICATION, 'test-application-id', expect.anything());
    });
  });

  describe('displaySelect', () => {
    describe('values are set', () => {
      beforeEach(() => {
        alertTriggerConditionStringComponent.values = [];
      });

      it('should return true with NOT_EQUALS operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'NOT_EQUALS' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeTruthy();
      });

      it('should return true with EQUALS operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'EQUALS' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeTruthy();
      });

      it('should return false with another operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'ANOTHER' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeFalsy();
      });
    });

    describe('values are not set', () => {
      beforeEach(() => {
        alertTriggerConditionStringComponent.values = undefined;
      });

      it('should return false with NOT_EQUALS operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'NOT_EQUALS' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeFalsy();
      });

      it('should return false with EQUALS operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'EQUALS' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeFalsy();
      });

      it('should return false with another operator', () => {
        alertTriggerConditionStringComponent.condition = { operator: 'ANOTHER' };
        expect(alertTriggerConditionStringComponent.displaySelect()).toBeFalsy();
      });
    });
  });

  /* This `describe` block is testing the `onSearch` method of the `AlertTriggerConditionStringComponent`. */
  describe('onSearch', () => {
    beforeEach(() => {
      alertTriggerConditionStringComponent.values = [{ value: 'default' }, { value: 'env' }, { value: 'org' }];
    });

    it('should show all values when searchTerm is empty', () => {
      alertTriggerConditionStringComponent.searchTerm = '';
      alertTriggerConditionStringComponent.onSearch();
      expect(alertTriggerConditionStringComponent.filteredValues).toEqual(alertTriggerConditionStringComponent.values);
    });

    it('should filter values by searchTerm (case-insensitive)', () => {
      alertTriggerConditionStringComponent.searchTerm = 'def';
      alertTriggerConditionStringComponent.onSearch();
      expect(alertTriggerConditionStringComponent.filteredValues).toEqual([{ value: 'default' }]);
    });

    it('should return no results if searchTerm does not match', () => {
      alertTriggerConditionStringComponent.searchTerm = 'zzz';
      alertTriggerConditionStringComponent.onSearch();
      expect(alertTriggerConditionStringComponent.filteredValues).toEqual([]);
    });
  });
});
