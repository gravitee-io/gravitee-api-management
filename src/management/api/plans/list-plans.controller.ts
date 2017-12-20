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
import _ = require('lodash');
import angular = require('angular');
import UserService from '../../../services/user.service';
import ApiService from "../../../services/api.service";
import NotificationService from "../../../services/notification.service";

class ApiListPlansController {
  private api: any;
  private plans: any;
  private dndEnabled: boolean;
  private statusFilters: string[];
  private selectedStatus: string[];
  private countByStatus: any;
  private filteredPlans: any;

  constructor(
    private $scope: ng.IScope,
    private $rootScope: ng.IRootScopeService,
    private $state: ng.ui.IStateService,
    private $stateParams: ng.ui.IStateParamsService,
    private $mdDialog: angular.material.IDialogService,
    private dragularService,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private ApiService: ApiService
  ) {
    'ngInject';
    this.dndEnabled = UserService.isUserHasPermissions(['api-plan-u']);
    this.statusFilters = ['staging', 'published', 'closed'];
    this.selectedStatus = ['published'];

    this.countByStatus = {};
  }

  $onInit() {
    let that = this;

    let d = document.querySelector('.plans');
    this.dragularService([d], {
      moves: function () {
        return that.dndEnabled;
      },
      scope: this.$scope,
      containersModel: this.plans,
      nameSpace: 'plan'
    });

    this.$scope.$on('dragulardrop', function(e, el, target, source, dragularList, index, targetModel, dropIndex) {
      let movedPlan = that.filteredPlans[index];
      movedPlan.order = dropIndex+1;

      that.ApiService.savePlan(that.$stateParams.apiId, movedPlan).then(function () {
        // sync list from server because orders has been changed
        that.list();
        that.NotificationService.show('Plans have been reordered successfully');
      });
    });

    if (this.$stateParams.state) {
      if (_.includes(this.statusFilters, this.$stateParams.state)) {
        this.changeFilter(this.$stateParams.state);
      } else {
        this.applyFilters();
      }
    } else {
      this.applyFilters();
    }
  }

  list() {
    this.ApiService.getApiPlans(this.$stateParams.apiId).then(response => {
      let that = this;
      this.$scope.$applyAsync(function(){
        that.plans.length = 0;
        Array.prototype.push.apply(that.plans, response.data);

        that.applyFilters();
      });
    });
  }

  changeFilter(statusFilter) {
    this.selectedStatus = statusFilter;
    this.dndEnabled = (statusFilter === 'published');

    if (_.includes(this.selectedStatus, statusFilter)) {
      _.pull(this.selectedStatus, statusFilter);
    } else {
      this.selectedStatus.push(statusFilter);
    }
    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {
        state: statusFilter
      }));
    this.applyFilters();
  }

  applyFilters() {
    this.countPlansByStatus();
    var that = this;
    this.filteredPlans = _.sortBy(
      _.filter(this.plans, function (plan: any) {
      return _.includes(that.selectedStatus, plan.status);
    }), "order");
  }

  addPlan() {
    this.$state.go('management.apis.detail.plans.new');
  }

  editPlan(plan: any) {
    this.$state.go('management.apis.detail.plans.plan', {planId: plan.id});
  }

  close(plan, ev) {
    this.ApiService.getPlanSubscriptions(this.$stateParams.apiId, plan.id).then( (response) => {
      this.$mdDialog.show({
        controller: 'DialogClosePlanController',
        template: require('./closePlan.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          apiId: this.$stateParams.apiId,
          plan: plan,
          subscriptions: response.data.page.size
        }
      }).then((plan) => {
        if (plan) {
          this.$scope.$parent.apiCtrl.checkAPISynchronization({id: this.$stateParams.apiId});
          this.selectedStatus = ['closed'];
          this.list();
        }
      }, function() {
        // You cancelled the dialog
      });
    });
  }

  publish(plan, ev) {
    this.$mdDialog.show({
        controller: 'DialogPublishPlanController',
        template: require('./publishPlan.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          plan: plan
        }
      }).then( (plan) => {
        if (plan) {
          this.ApiService.publishPlan(this.$stateParams.apiId, plan.id).then( () => {
            this.NotificationService.show('Plan ' + plan.name + ' has been published');
            this.$rootScope.$broadcast("planChangeSuccess", { state: "published"});
            this.$scope.$parent.apiCtrl.checkAPISynchronization({id: this.$stateParams.apiId});
            this.selectedStatus = ['published'];
            this.list();
          });
        }
      }, function() {
        // You cancelled the dialog
      });
  }

  countPlansByStatus() {
    this.countByStatus = _.countBy(this.plans, 'status');
  }

}

export default ApiListPlansController;
