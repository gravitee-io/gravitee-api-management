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
import { describe, expect, test } from '@jest/globals';
import { IControllerService } from 'angular';
import * as _ from 'lodash';

import { setupAngularJsTesting } from '../../../../../../jest.setup';

setupAngularJsTesting();

describe('management.api.endpointConfiguration', () => {
  let $controller: IControllerService;
  let endpointConfigurationController: any;
  let $scope: any;
  let api: any;

  beforeEach(inject((_$controller_, _$rootScope_, _$stateParams_) => {
    $controller = _$controller_;
    _$stateParams_.groupName = 'default';
    _$stateParams_.endpointName = 'default';
    api = {
      proxy: {
        groups: [
          {
            name: _$stateParams_.groupName,
            endpoints: [
              {
                name: _$stateParams_.endpointName,
                type: 'http',
                headers: [
                  {
                    name: 'x-gravitee',
                    value: 'foobar',
                  },
                ],
              },
            ],
          },
        ],
      },
    };

    $scope = _$rootScope_.$new();
    $scope.formEndpoint = {
      $setDirty: jest.fn(),
      $setValidity: jest.fn(),
    };
    $scope.$parent.apiCtrl = {
      api,
    };
    const bindings = {};
    endpointConfigurationController = $controller(
      'ApiEndpointController',
      {
        $scope,
        $stateParams: _$stateParams_,
        resolvedTenants: {},
        resolvedConnectors: {
          data: [
            {
              name: 'http-connector',
              supportedTypes: ['http', 'grpc'],
              schema: JSON.stringify({ headers: [] }),
            },
          ],
        },
      },
      bindings,
    );
    endpointConfigurationController.$onInit();
  }));

  test('should find endpoint by name', () => {
    const endpointByName = endpointConfigurationController.findEndpointByName(api.proxy.groups[0], 'default');
    expect(endpointByName).toBeDefined();
  });

  test('should find current schema', () => {
    const schema = endpointConfigurationController.findCurrentSchema();
    expect(schema).toBeDefined();
  });

  test('should update endpoint and preserve reference', () => {
    const detail = {
      values: {
        timeout: 5000,
      },
    };
    const setDirtySpy = jest.spyOn(endpointConfigurationController.$scope.formEndpoint, '$setDirty');
    const setValiditySpy = jest.spyOn(endpointConfigurationController.$scope.formEndpoint, '$setValidity');
    endpointConfigurationController.updateEndpoint({ detail });
    expect(endpointConfigurationController.$scope.endpoint.name).toEqual('default');
    expect(endpointConfigurationController.$scope.endpoint.timeout).toEqual(5000);
    expect(endpointConfigurationController.$scope.endpoint.headers).toBeUndefined();
    expect(
      endpointConfigurationController.api.proxy.groups[0].endpoints[0] === endpointConfigurationController.$scope.endpoint,
    ).toBeTruthy();
    const group: any = endpointConfigurationController.findGroupByName('default');
    expect(_.includes(group.endpoints, endpointConfigurationController.$scope.endpoint)).toBeTruthy();
    expect(group.endpoints.length).toEqual(1);
    expect(setDirtySpy).toHaveBeenCalled();
    expect(setValiditySpy).toHaveBeenCalledWith('endpoint', true);
  });

  test('should not update endpoint with validation error', () => {
    const detail = {
      values: {
        timeout: 5000,
      },
      validation: {
        errors: ['timeout must be < 5000'],
      },
    };
    const setDirtySpy = jest.spyOn(endpointConfigurationController.$scope.formEndpoint, '$setDirty');
    const setValiditySpy = jest.spyOn(endpointConfigurationController.$scope.formEndpoint, '$setValidity');
    endpointConfigurationController.updateEndpoint({ detail });
    expect(endpointConfigurationController.$scope.endpoint.name).toEqual('default');
    expect(endpointConfigurationController.$scope.endpoint.timeout).toBeUndefined();
    expect(endpointConfigurationController.$scope.endpoint.headers).toBeDefined();
    expect(
      endpointConfigurationController.api.proxy.groups[0].endpoints[0] === endpointConfigurationController.$scope.endpoint,
    ).toBeTruthy();
    const group: any = endpointConfigurationController.findGroupByName('default');
    expect(_.includes(group.endpoints, endpointConfigurationController.$scope.endpoint)).toBeTruthy();
    expect(group.endpoints.length).toEqual(1);
    expect(setDirtySpy).toHaveBeenCalled();
    expect(setValiditySpy).toHaveBeenCalledWith('endpoint', false);
  });
});
