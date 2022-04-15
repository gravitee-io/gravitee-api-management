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

import InstanceMonitoringController from './instance-monitoring.controller';

import { setupAngularJsTesting } from '../../../../../jest.setup';

setupAngularJsTesting();

describe('InstanceMonitoringComponent', () => {
  let $componentController: IComponentControllerService;
  let instanceMonitoringComponent: InstanceMonitoringController;

  beforeEach(inject((_$stateParams_, _$componentController_) => {
    $componentController = _$componentController_;
    instanceMonitoringComponent = $componentController('instanceMonitoring', null, {});
  }));

  describe('humanizeSize', () => {
    it.each([
      [Infinity, 1, '-'],
      [42, 1, '42.0 bytes'],
      [1050, 1, '1.0 kB'],
      [100056, 1, '97.7 kB'],
      [4096000000, 2, '3.81 GB'],
      [0, 1, '0.0 bytes'],
      [42, undefined, '42.0 bytes'],
      [42, 0, '42 bytes'],
    ])('should convert %p bytes with precision %p into %p', (bytes, precision, result) => {
      expect(instanceMonitoringComponent.humanizeSize(bytes, precision)).toBe(result);
    });
  });

  describe('ratio', () => {
    it.each([
      [42, 1, 4200],
      [42, 0, '-'],
      [0, 42, 0],
      [115312310, 240, 48046796],
      [240, 115312310, 0],
    ])('should compute ratio of %p on %p into %p', (val1, val2, result) => {
      expect(instanceMonitoringComponent.ratio(val1, val2)).toBe(result);
    });
  });
});
