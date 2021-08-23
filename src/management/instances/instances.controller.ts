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
import { StateService } from '@uirouter/core';

import InstancesService from '../../services/instances.service';

interface IInstancesScope extends ng.IScope {
  showHistory: boolean;

  switchDisplayInstances(): void;
}

class InstancesController {
  private instances: any;
  private _displayEmptyMode: boolean;
  private searchGatewayInstances: string;
  private query: any;
  private lastFrom: any;
  private lastTo: any;

  constructor(private $scope: IInstancesScope, private InstancesService: InstancesService, private $state: StateService) {
    'ngInject';
  }

  $onInit() {
    this.query = {
      limit: 10,
      page: 1,
    };

    this.searchInstances = this.searchInstances.bind(this);

    this.searchGatewayInstances = '';
    this._displayEmptyMode = this.instances.content.length === 0;

    this.$scope.showHistory = false;

    this.$scope.switchDisplayInstances = () => {
      this.instances.content = [];
      this.$scope.showHistory = !this.$scope.showHistory;

      if (this.$scope.showHistory) {
        const now = Date.now();
        this.$state.params.from = now - 1000 * 60 * 60 * 24;
        this.$state.params.to = now + 1000 * 60;
      } else {
        this.searchInstances();
      }
    };
  }

  getOSIcon(osName) {
    if (osName) {
      const lowerOSName = osName.toLowerCase();
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

  onTimeframeChange(timeframe) {
    this.lastFrom = timeframe.from;
    this.lastTo = timeframe.to;

    this.searchInstances();
  }

  displayEmptyMode() {
    return this.instances.length === 0;
  }

  searchInstances() {
    this.InstancesService.search(
      this.$scope.showHistory,
      this.$scope.showHistory ? this.lastFrom : 0,
      this.$scope.showHistory ? this.lastTo : 0,
      this.$scope.showHistory ? this.query.page - 1 : 0,
      this.$scope.showHistory ? this.query.limit : 100,
    ).then((response) => {
      this.instances = response.data;
      this._displayEmptyMode = this.instances.content.length === 0;
    });
  }
}

export default InstancesController;
