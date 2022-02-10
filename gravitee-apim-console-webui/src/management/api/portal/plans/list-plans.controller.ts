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
import angular from 'angular';
import * as _ from 'lodash';

import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';
import UserService from '../../../../services/user.service';

class ApiListPlansController {
  private api: any;
  private plans: any;
  private groups: any[];
  private dndEnabled: boolean;
  private statusFilters: string[];
  private selectedStatus: string[];
  private countByStatus: any;
  private filteredPlans: any;
  private isApiDeprecated: boolean;
  private creationEmptyMessage: string;

  constructor(
    private $scope,
    private $rootScope: ng.IRootScopeService,
    private $state: StateService,
    private $stateParams,
    private $mdDialog: angular.material.IDialogService,
    private dragularService,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private ApiService: ApiService,
  ) {
    'ngInject';
    this.dndEnabled = UserService.isUserHasPermissions(['api-plan-u']);
    this.statusFilters = ['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'];
    this.selectedStatus = ['PUBLISHED'];

    this.countByStatus = {};
  }

  $onInit() {
    const d = document.querySelector('.plans');
    this.dragularService([d], {
      moves: () => this.dndEnabled,
      scope: this.$scope,
      containersModel: this.plans,
      nameSpace: 'plan',
    });

    this.$scope.$on('dragulardrop', (e, el, target, source, dragularList, index, targetModel, dropIndex) => {
      const movedPlan = this.filteredPlans[index];
      movedPlan.order = dropIndex + 1;

      this.ApiService.savePlan(this.api, movedPlan).then(() => {
        // sync list from server because orders has been changed
        this.list();
        this.NotificationService.show('Plans have been reordered successfully');
      });
    });

    if (this.$stateParams.state) {
      const stateQueryParam = this.$stateParams.state.toUpperCase();
      if (_.includes(this.statusFilters, stateQueryParam)) {
        this.changeFilter(stateQueryParam);
      } else {
        this.applyFilters();
      }
    } else {
      this.applyFilters();
    }

    this.isApiDeprecated = this.api.lifecycle_state === 'DEPRECATED';
    this.creationEmptyMessage = this.isApiDeprecated ? 'The API is deprecated' : 'Start creating a plan';

    this.refreshPlansExcludedGroupsNames();
  }

  canDesign(plan) {
    return this.ApiService.isV2(this.api) && plan.status !== 'CLOSED';
  }

  design(plan) {
    this.$state.go('management.apis.detail.design.flowsNg', { apiId: this.api.id, flows: `${plan.id}_0` });
  }

  list() {
    this.ApiService.getApiPlans(this.$stateParams.apiId).then((response) => {
      this.$scope.$applyAsync(() => {
        this.plans.length = 0;
        Array.prototype.push.apply(this.plans, response.data);

        this.refreshPlansExcludedGroupsNames();
        this.applyFilters();
      });
    });
  }

  changeFilter(statusFilter) {
    this.selectedStatus = statusFilter;
    this.dndEnabled = statusFilter === 'PUBLISHED' && this.UserService.isUserHasPermissions(['api-plan-u']);

    if (_.includes(this.selectedStatus, statusFilter)) {
      _.pull(this.selectedStatus, statusFilter);
    } else {
      this.selectedStatus.push(statusFilter);
    }
    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {
        state: statusFilter,
      }),
    );
    this.applyFilters();
  }

  applyFilters() {
    this.countPlansByStatus();
    this.filteredPlans = _.sortBy(
      _.filter(this.plans, (plan: any) => {
        return _.includes(this.selectedStatus, plan.status);
      }),
      'order',
    );
  }

  addPlan() {
    this.$state.go('management.apis.detail.portal.plans.new');
  }

  editPlan(plan: any) {
    this.$state.go('management.apis.detail.portal.plans.plan', { planId: plan.id });
  }

  close(plan) {
    this.ApiService.getAllPlanSubscriptions(this.$stateParams.apiId, plan.id).then((response) => {
      const subscriptions = response.data.page.size;
      let msg = '';
      if (plan.security === 'KEY_LESS') {
        msg = 'A keyless plan may have consumers. <br/>' + 'By closing this plan you will remove free access to this API.';
      } else {
        if (subscriptions === 0) {
          msg = 'No subscription is associated to this plan. You can delete it safely.';
        } else if (subscriptions > 0) {
          msg =
            'There are <code>' +
            subscriptions +
            '</code> subscription(s) associated to this plan.<br/>' +
            'By closing this plan, all relative active subscriptions will also be closed.';
        }
      }
      let confirmButton = 'Yes, close this plan.';
      if (subscriptions === 0 && plan.security === 'API_KEY') {
        confirmButton = 'Yes, delete this plan';
      }
      this.$mdDialog
        .show({
          controller: 'DialogConfirmAndValidateController',
          controllerAs: 'ctrl',
          template: require('../../../../components/dialog/confirmAndValidate.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Would you like to close plan "' + plan.name + '"?',
            warning: 'This operation is irreversible.',
            msg: msg,
            validationMessage: 'Please, type in the name of the plan <code>' + plan.name + '</code> to confirm.',
            validationValue: plan.name,
            confirmButton: confirmButton,
          },
        })
        .then((response) => {
          if (response) {
            this.ApiService.closePlan(this.api, plan.id).then(() => {
              this.NotificationService.show('Plan ' + plan.name + ' has been closed');
              this.$rootScope.$broadcast('planChangeSuccess', { state: 'CLOSED' });
              this.$scope.$parent.apiCtrl.checkAPISynchronization({ id: this.$stateParams.apiId });
              this.selectedStatus = ['CLOSED'];
              this.list();
            });
          }
        });
    });
  }

  deprecate(plan) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to deprecate plan "' + plan.name + '"?',
          msg: 'By deprecating this plan, users will no more be able to subscribe to it.',
          confirmButton: 'Deprecate',
        },
      })
      .then((response) => {
        if (response) {
          this.ApiService.deprecatePlan(this.api, plan.id).then(() => {
            this.NotificationService.show('Plan ' + plan.name + ' has been deprecated');
            this.$rootScope.$broadcast('planChangeSuccess', { state: 'DEPRECATED' });
            this.$scope.$parent.apiCtrl.checkAPISynchronization({ id: this.$stateParams.apiId });
            this.selectedStatus = ['PUBLISHED'];
            this.list();
          });
        }
      });
  }

  publish(plan, ev) {
    this.$mdDialog
      .show({
        controller: 'DialogPublishPlanController',
        template: require('./publishPlan.dialog.html'),
        parent: angular.element(document.body),
        targetEvent: ev,
        clickOutsideToClose: true,
        locals: {
          plan: plan,
        },
      })
      .then(
        (plan) => {
          if (plan) {
            this.ApiService.publishPlan(this.api, plan.id).then(() => {
              this.NotificationService.show('Plan ' + plan.name + ' has been published');
              this.$rootScope.$broadcast('planChangeSuccess', { state: 'PUBLISHED' });
              this.$scope.$parent.apiCtrl.checkAPISynchronization({ id: this.$stateParams.apiId });
              this.selectedStatus = ['PUBLISHED'];
              this.list();
            });
          }
        },
        () => {
          // You cancelled the dialog
        },
      );
  }

  countPlansByStatus() {
    this.countByStatus = _.countBy(this.plans, 'status');
  }

  refreshPlansExcludedGroupsNames() {
    this.plans.forEach(
      (plan) =>
        (plan.excluded_groups_names = plan.excluded_groups?.map(
          (excludedGroupId) => this.groups.find((apiGroup) => apiGroup.id === excludedGroupId)?.name,
        )),
    );
  }
}

export default ApiListPlansController;
