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
class DashboardDirective {
  constructor() {
    let directive = {
      restrict: 'E',
      scope: {
        model: '='
      },
      templateUrl: 'app/components/analytics/dashboard.html',
      controller: DashboardController,
      controllerAs: 'dashboardCtrl',
      bindToController: true
    };

    return directive;
  }
}

class DashboardController {
  constructor($scope, $timeout, $rootScope) {
    'ngInject';

    let _that = this;

    $scope.dashboardOptions = {
      margins: [10, 10],
      columns: 6,
      swapping: false,
      draggable: {
        enable: true,
        handle: 'md-card-title'
      },
      resizable: {
        enabled: true,
        stop: function () {
          $scope.$broadcast('onWidgetResize');
        }
      },
      rowHeight: 330
    };

    _.each(this.model, function(widget) {
      widget.$uid = _that.guid();
    });

    $timeout(function(){
      $rootScope.$broadcast('timeframeReload');
    });
  }

  guid() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
  }
}

export default DashboardDirective;
