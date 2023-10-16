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
import { StateParams } from '@uirouter/core';

import TicketsListController from './support/tickets-list.controller';

import AnalyticsService from '../services/analytics.service';
import { ApiService } from '../services/api.service';
import ApplicationService from '../services/application.service';
import DashboardService from '../services/dashboard.service';
import InstancesService from '../services/instances.service';
import TicketService from '../services/ticket.service';
import { ApimFeature } from '../shared/components/gio-license/gio-license-data';

function managementRouterConfig($stateProvider) {
  $stateProvider
    .state('management', {
      redirectTo: 'home',
      template: '<div ui-view layout="column" flex></div>',
      parent: 'withSidenav',
      controller: [
        '$rootScope',
        'Constants',
        function ($rootScope, Constants) {
          $rootScope.consoleTitle = Constants.org.settings.management.title;
        },
      ],
      controllerAs: '$ctrl',
    })
    .state('management.instances', {
      abstract: true,
      url: '/instances',
      template: '<div ui-view></div>',
    })
    .state('management.instances.list', {
      url: '/',
      component: 'instances',
      resolve: {
        instances: [
          'InstancesService',
          (InstancesService: InstancesService) => InstancesService.search().then((response) => response.data),
        ],
      },
      data: {
        perms: {
          only: ['environment-instance-r'],
        },
        docs: {
          page: 'management-gateways',
        },
      },
    })
    // 'management.instances.detail' has been moved to src/management/instances/instance-details/instance-details.module.ts

    .state('management.logs', {
      url: '/logs?from&to&q&page&size',
      component: 'platformLogs',
      data: {
        menu: null,
        devMode: true,
        perms: {
          only: ['environment-platform-r'],
        },
        docs: {
          page: 'management-api-logs',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
      },
      resolve: {
        apis: ['ApiService', (ApiService: ApiService) => ApiService.list(null, false, 1, null, null, null, 10)],
        applications: ['ApplicationService', (ApplicationService: ApplicationService) => ApplicationService.list(['owner', 'picture'])],
      },
    })
    .state('management.log', {
      url: '/logs/:logId?timestamp&from&to&q&page&size',
      component: 'platformLog',
      resolve: {
        log: [
          '$stateParams',
          'AnalyticsService',
          ($stateParams, AnalyticsService: AnalyticsService) =>
            AnalyticsService.getLog($stateParams.logId, $stateParams.timestamp).then((response) => response.data),
        ],
      },
      data: {
        devMode: true,
        menu: null,
        perms: {
          only: ['environment-platform-r'],
        },
        docs: {
          page: 'management-api-log',
        },
      },
    })
    .state('management.audit', {
      url: '/audit',
      component: 'ngEnvAudit',
      data: {
        useAngularMaterial: true,
        requireLicense: {
          license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
          redirect: 'management',
        },
        perms: {
          only: ['environment-audit-r'],
        },
        docs: {
          page: 'management-audit',
        },
      },
    })
    .state('management.messages', {
      url: '/messages',
      component: 'ngMessages',
      data: {
        docs: {
          page: 'management-messages',
        },
      },
    })
    .state('management.tasks', {
      url: '/tasks',
      component: 'tasks',
      data: {
        docs: {
          page: 'management-tasks',
        },
      },
    })
    .state('management.support', {
      template: '<div ui-view></div>',
    })
    .state('management.support.create', {
      url: '/support',
      template: require('./support/ticket.html'),
      controller: 'SupportTicketController',
      controllerAs: 'supportTicketCtrl',
    })
    .state('management.support.tickets', {
      url: '/support/tickets?page&size&order',
      template: require('./support/tickets-list.html'),
      controller: TicketsListController,
      controllerAs: 'ticketsListCtrl',
      params: {
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
        order: {
          type: 'string',
          dynamic: true,
        },
      },
    })
    .state('management.support.ticket', {
      url: '/support/tickets/:ticketId?page&size&order',
      component: 'ticketDetail',
      resolve: {
        ticket: [
          '$stateParams',
          'TicketService',
          ($stateParams: StateParams, TicketService: TicketService) =>
            TicketService.getTicket($stateParams.ticketId).then((response) => response.data),
        ],
      },
    })
    .state('management.analytics', {
      url: '/platform?from&to&q&dashboard',
      template: require('./dashboard-ajs/analytics-dashboard/analytics-dashboard.html'),
      controller: 'AnalyticsDashboardController',
      controllerAs: '$ctrl',
      resolve: {
        dashboards: [
          'DashboardService',
          (DashboardService: DashboardService) => DashboardService.list('PLATFORM').then((response) => response.data),
        ],
      },
      data: {
        perms: {
          only: ['environment-platform-r'],
        },
        docs: {
          page: 'management-dashboard-analytics',
        },
      },
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        dashboard: {
          type: 'string',
          dynamic: true,
        },
      },
    });
}
managementRouterConfig.$inject = ['$stateProvider'];

export default managementRouterConfig;
