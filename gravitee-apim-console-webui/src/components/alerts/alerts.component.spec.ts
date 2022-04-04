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

import { setupAngularJsTesting } from '../../../jest.setup';
import { Alert, Scope } from '../../../src/entities/alert';

setupAngularJsTesting();

describe('AlertsComponent', () => {
  let $componentController: IComponentControllerService;
  let $stateParams;
  let alertsComponent: any;

  beforeEach(inject((_$stateParams_, _$componentController_) => {
    $componentController = _$componentController_;
    $stateParams = _$stateParams_;
    alertsComponent = $componentController('alertsComponent', null, {});
  }));

  describe('enhanceAlert', () => {
    const alert = new Alert('alert', 'INFO', 'source', 'my alert', 'test', undefined, undefined);

    it('should set reference_type to ENVIRONMENT', () => {
      $stateParams.apiId = undefined;
      $stateParams.applicationId = undefined;

      alertsComponent.enhanceAlert(alert);

      expect(alert.reference_type).toEqual(Scope.ENVIRONMENT);
    });

    it('should set reference_type to API', () => {
      $stateParams.apiId = 'test-api';
      $stateParams.applicationId = undefined;

      alertsComponent.enhanceAlert(alert);

      expect(alert.reference_type).toEqual(Scope.API);
    });

    it('should set reference_type to APPLICATION', () => {
      $stateParams.apiId = undefined;
      $stateParams.applicationId = 'test-application';

      alertsComponent.enhanceAlert(alert);

      expect(alert.reference_type).toEqual(Scope.APPLICATION);
    });
  });
});
