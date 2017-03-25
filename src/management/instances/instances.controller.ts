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
import * as _ from 'lodash';

interface IInstancesScope extends ng.IScope {

  switchDisplayInstances(): void;

  displayAllInstances: boolean;
}

class InstancesController {
  private instances: any;
  private startedInstances: any;
  private _displayEmptyMode: boolean;
  private searchGatewayInstances: string;

  constructor(
    private $scope: IInstancesScope) {
		'ngInject';
	}

  $onInit() {
    this.searchGatewayInstances = '';
    this.startedInstances = _.filter(this.instances, { 'state': 'started'});
    this._displayEmptyMode = this.startedInstances.length === 0;

    this.$scope.displayAllInstances = false;

    let that = this;
    this.$scope.switchDisplayInstances = function() {
      that.$scope.displayAllInstances = !that.$scope.displayAllInstances;

      if (!that.$scope.displayAllInstances) {
        that._displayEmptyMode = that.startedInstances.length === 0;
      } else {
        that._displayEmptyMode = that.instances.length === 0;
      }
    };
  }

  getOSIcon(osName) {
    if (osName) {
      var lowerOSName = osName.toLowerCase();
      if (lowerOSName.indexOf('mac') >= 0) {
        return 'apple';
      } else if (lowerOSName.indexOf('nix') >= 0 || lowerOSName.indexOf('nux') >= 0 || lowerOSName.indexOf('aix') >= 0) {
        return 'desktop_windows';
      } else if (lowerOSName.indexOf('win') >= 0) {
        return 'windows';
      }
    }
    return 'desktop_windows';
  }

  displayEmptyMode() {
    return this.instances.length === 0;
  }
}

export default InstancesController;
