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

import { setupAngularJsTesting } from '../../../../../../jest.setup';
import { Metrics, Scope } from '../../../../../entities/alert';

setupAngularJsTesting();

describe('AlertTriggerConditionStringComponent', () => {
  let $componentController: IComponentControllerService;
  let $state;
  let alertTriggerConditionStringComponent: any;

  beforeEach(inject((_$state_, _$componentController_) => {
    $componentController = _$componentController_;
    $state = _$state_;
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
      $state.params.apiId = undefined;
      $state.params.applicationId = undefined;

      alertTriggerConditionStringComponent.$onInit();

      expect(metricLoaderSpy).toHaveBeenCalledWith(Scope.ENVIRONMENT, undefined, expect.anything());
    });

    it('should call loader with API type', () => {
      $state.params.apiId = 'test-api-id';
      $state.params.applicationId = undefined;

      alertTriggerConditionStringComponent.$onInit();

      expect(metricLoaderSpy).toHaveBeenCalledWith(Scope.API, 'test-api-id', expect.anything());
    });

    it('should call loader with APPLICATION type', () => {
      $state.params.apiId = undefined;
      $state.params.applicationId = 'test-application-id';

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
});
