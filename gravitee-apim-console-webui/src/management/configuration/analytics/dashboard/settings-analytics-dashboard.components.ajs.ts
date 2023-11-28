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
import angular from 'angular';
import * as _ from 'lodash';
import { Router } from '@angular/router';

import DashboardService from '../../../../services/dashboard.service';
import NotificationService from '../../../../services/notification.service';
const SettingsAnalyticsDashboardComponentAjs: ng.IComponentOptions = {
  template: require('./settings-analytics-dashboard.html'),
  controller: [
    'DashboardService',
    'NotificationService',
    '$scope',
    '$rootScope',
    '$mdDialog',
    '$timeout',
    'ngRouter',
    function (
      DashboardService: DashboardService,
      NotificationService: NotificationService,
      $scope,
      $rootScope,
      $mdDialog,
      $timeout,
      ngRouter: Router,
    ) {
      let previousPristine = true;
      this.fields = DashboardService.getIndexedFields();
      this.$rootScope = $rootScope;
      this.ngRouter = ngRouter;
      this.updateMode = true;
      this.$onInit = () => {
        this.editMode = !!this.activatedRoute.snapshot.params.dashboardId;
        if (this.activatedRoute.snapshot.params.dashboardId) {
          DashboardService.get(this.activatedRoute.snapshot.params.dashboardId).then((response) => {
            this.dashboard = response.data;
            if (this.dashboard.definition) {
              this.dashboard.definition = JSON.parse(this.dashboard.definition);
              _.forEach(this.dashboard.definition, (widget) => {
                _.merge(widget, DashboardService.getChartService());
              });
            }
            $scope.$watch(
              '$ctrl.dashboard.definition',
              (newDefinition, oldDefinition) => {
                if (!_.isEqual(newDefinition, oldDefinition)) {
                  this.formDashboard.$setDirty();
                }
              },
              true,
            );
          });
        } else {
          this.dashboard = {
            reference_type: this.activatedRoute.snapshot.params.type,
            reference_id: 'DEFAULT',
            enabled: true,
            definition: [],
          };
        }
      };

      this.save = () => {
        let savePromise;
        const clonedDashboard = _.cloneDeep(this.dashboard);
        if (clonedDashboard.definition) {
          _.forEach(clonedDashboard.definition, (widget) => {
            if (widget.chart) {
              if (widget.chart.service) {
                delete widget.chart.service;
              }
              if (widget.chart.request) {
                delete widget.chart.request.interval;
                delete widget.chart.request.from;
                delete widget.chart.request.to;
              }
            }
          });
          clonedDashboard.definition = angular.toJson(clonedDashboard.definition);
        }
        if (this.editMode) {
          savePromise = DashboardService.update(clonedDashboard);
        } else {
          savePromise = DashboardService.create(clonedDashboard);
        }
        savePromise.then((response) => {
          this.formDashboard.$setPristine();
          NotificationService.show(`Dashboard ${this.editMode ? 'updated' : 'created'} with success`);
          return this.ngRouter.navigate(['..', response.data.id], { relativeTo: this.activatedRoute });
        });
      };

      this.reset = () => {
        this.$onInit();
      };

      this.displayPreview = () => {
        if (this.dashboard && this.dashboard.definition && this.dashboard.definition.length) {
          return _.every(this.dashboard.definition, (definition) => {
            return definition.chart.type;
          });
        }
        return false;
      };

      this.addWidget = () => {
        this.dashboard.definition.push(DashboardService.getChartService());
      };

      this.showQueryFilterInformation = () => {
        $mdDialog.show({
          controller: 'DialogQueryFilterInformationController',
          controllerAs: 'ctrl',
          template: require('./query-filter-information.dialog.html'),
          parent: angular.element(document.body),
          clickOutsideToClose: true,
        });
      };

      this.tooglePreview = () => {
        this.updateMode = !this.updateMode;
        if (this.updateMode) {
          if (previousPristine) {
            this.formDashboard.$setPristine();
          } else {
            $timeout(() => {
              this.formDashboard.$setDirty();
            }, 100);
          }
        } else {
          previousPristine = this.formDashboard.$pristine;
        }
      };

      this.backToDashboards = () => {
        this.ngRouter.navigate(['../../..'], { relativeTo: this.activatedRoute });
      };
    },
  ],
};

export default SettingsAnalyticsDashboardComponentAjs;
