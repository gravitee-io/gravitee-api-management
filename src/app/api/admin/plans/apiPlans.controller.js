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
class ApiPlansController {
  constructor(resolvedPlans, $mdSidenav, $scope, ApiService, $stateParams, NotificationService) {
    'ngInject';
    this.plans = resolvedPlans.data;
    this.$mdSidenav = $mdSidenav;
    this.$scope = $scope;
    this.ApiService = ApiService;
    this.$stateParams = $stateParams;
    this.NotificationService = NotificationService;
    $scope.planEdit = true;

    this.resetPlan();

    var that = this;
    $scope.configure = function (plan) {
      that.resetPlan();
      if (!$mdSidenav('plan-edit').isOpen()) {
        $scope.plan = plan;
        if ($scope.plan.paths['/']) {
          _.forEach($scope.plan.paths['/'], function (path) {
            if (path['rate-limit']) {
              $scope.rateLimit = path['rate-limit'].rate;
            }
            if (path.quota) {
              $scope.quota = path.quota.quota;
            }
            if (path['resource-filtering']) {
              $scope.resourceFiltering.whitelist = path['resource-filtering'].whitelist;
            }
          });
        }
        $mdSidenav('live-preview').toggle();
        $mdSidenav('plan-edit').toggle();
      }
    };

    $scope.timeUnits = ['SECONDS', 'MINUTES', 'HOURS', 'DAYS'];
    $scope.methods = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];

    this.resetResourceFiltering();

    $scope.$watch('livePreviewIsOpen', function (livePreviewIsOpen) {
      if (livePreviewIsOpen === false && $mdSidenav('plan-edit').isOpen()) {
        $mdSidenav('plan-edit').toggle();
      }
    });
  }

  resetResourceFiltering() {
    this.$scope.resourceFiltering = {
      whitelist: []
    };
  }

  resetPlan() {
    this.$scope.plan = {characteristics: []};
    this.resetRateLimit();
    this.resetQuota();
    this.resetResourceFiltering();
  }

  resetRateLimit() {
    delete this.$scope.rateLimit;
  }

  resetQuota() {
    delete this.$scope.quota;
  }

  addPlan() {
    this.resetPlan();
    this.$mdSidenav('plan-edit').toggle();
    this.$mdSidenav('live-preview').toggle();
  }

  save() {
    var that = this;

    this.$scope.plan.paths = {
      '/': []
    };
    // set resource filtering whitelist
    _.remove(this.$scope.resourceFiltering.whitelist, function (whitelistItem) {
      return !whitelistItem.pattern;
    });
    if (this.$scope.resourceFiltering.whitelist.length) {
      that.$scope.plan.paths['/'].push({
        'methods': that.$scope.methods,
        'resource-filtering': {
          'whitelist': this.$scope.resourceFiltering.whitelist
        }
      });
    }
    // set rate limit policy
    if (this.$scope.rateLimit && this.$scope.rateLimit.limit) {
      this.$scope.plan.paths['/'].push({
        'methods': this.$scope.methods,
        'rate-limit': {
          'rate': this.$scope.rateLimit
        }
      });
    }
    // set quota policy
    if (this.$scope.quota && this.$scope.quota.limit) {
      this.$scope.plan.paths['/'].push({
        'methods': this.$scope.methods,
        'quota': {
          'quota': this.$scope.quota,
          'addHeaders': true
        }
      });
    }

    that.ApiService.savePlan(that.$stateParams.apiId, that.$scope.plan).then(function () {
      that.$scope.$parent.apiCtrl.checkAPISynchronization({id: that.$stateParams.apiId});
      that.NotificationService.show('The plan ' + that.$scope.plan.name + ' has been saved with success');
      that.ApiService.getApiPlans(that.$stateParams.apiId).then(function (response) {
        that.plans = response.data;
        that.$mdSidenav('plan-edit').toggle();
        that.$mdSidenav('live-preview').toggle();
      });
    });
  }
}

export default ApiPlansController;
