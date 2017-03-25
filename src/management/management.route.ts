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
import InstancesService from '../services/instances.service';

function managementRouterConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management', {
      url: '/management',
      redirectTo: 'management.apis.list',
      parent: 'withSidenav'
    })
    .state('management.instances', {
      abstract: true,
      url: '/instances',
      template: '<div ui-view></div>'
    })
    .state('management.instances.list', {
      url: '/',
      component: 'instances',
      resolve: {
        instances: (InstancesService: InstancesService) => InstancesService.list().then(response => response.data)
      },
      data: {
        menu: {
          label: 'Instances',
          icon: 'developer_dashboard',
          firstLevel: true,
          order: 30
        },
        roles: ['ADMIN']
      }
    })
    .state('management.instances.detail', {
      abstract: true,
      url: '/:instanceId',
      component: 'instance',
      resolve: {
        instance: ($stateParams: ng.ui.IStateParamsService, InstancesService: InstancesService) =>
          InstancesService.get($stateParams['instanceId']).then(response => response.data)
      }
    })
    .state('management.instances.detail.environment', {
      url: '/environment',
      component: 'instanceEnvironment',
      data: {
        menu: {
          label: 'Environment',
          icon: 'computer'
        }
      }
    })
    .state('management.instances.detail.monitoring', {
      url: '/monitoring',
      component: 'instanceMonitoring',
      data: {
        menu: {
          label: 'Monitoring',
          icon: 'graphic_eq'
        },
      },
      resolve: {
        monitoringData: ($stateParams: ng.ui.IStateParamsService, InstancesService: InstancesService, instance: any) =>
          InstancesService.getMonitoringData($stateParams['instanceId'], instance.id).then(response => response.data)
      }
    })
    .state('management.platform', {
      url: '/platform?from&to',
      template: require('./platform/dashboard/dashboard.html'),
      controller: 'DashboardController',
      controllerAs: 'dashboardCtrl',
      data: {
        menu: {
          label: 'Dashboard',
          icon: 'show_chart',
          firstLevel: true,
          order: 40
        },
        roles: ['ADMIN']
      },
      params: {
        from: {
          type: 'int',
          dynamic: true
        },
        to: {
          type: 'int',
          dynamic: true
        }
      }
    });
}

export default managementRouterConfig;
