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
import ApplicationService from '../../services/applications.service';
import GroupService from '../../services/group.service';

export default applicationsConfig;

function applicationsConfig($stateProvider: ng.ui.IStateProvider) {
  'ngInject';
  $stateProvider
    .state('management.applications', {
      abstract: true,
      url: '/applications'
    })
    .state('management.applications.list', {
      url: '/',
      component: 'applications',
      data: {
        menu: {
          label: 'Applications',
          icon: 'list',
          firstLevel: true,
          order: 20
        },
        roles: ['USER', 'ADMIN', 'API_CONSUMER', 'API_PUBLISHER'],
        devMode: true
      },
      resolve: {
        applications: (ApplicationService: ApplicationService) => ApplicationService.list().then(response => response.data)
      },
    })
    .state('management.applications.portal', {
      abstract: true,
      url: '/:applicationId',
      component: 'application',
      resolve: {
        application: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.get($stateParams['applicationId']).then(response => response.data)
      }
    })
    .state('management.applications.portal.general', {
      url: '/',
      component: 'applicationGeneral',
      data: {
        menu: {
          label: 'Global settings',
          icon: 'blur_on'
        },
        devMode: true
      },
      resolve: {
        application: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.get($stateParams['applicationId']).then(response => response.data)
      }
    })
    .state('management.applications.portal.subscriptions', {
      url: '/subscriptions',
      component: 'applicationSubscriptions',
      resolve: {
        subscriptions: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.listSubscriptions($stateParams['applicationId']).then(response => response.data)
      },
      data: {
        menu: {
          label: 'Subscriptions',
          icon: 'vpn_key'
        },
        devMode: true
      }
    })
    .state('management.applications.portal.members', {
      url: '/members',
      component: 'applicationMembers',
      resolve: {
        members: ($stateParams: ng.ui.IStateParamsService, ApplicationService: ApplicationService) =>
          ApplicationService.getMembers($stateParams['applicationId']).then(response => response.data),
        groupMembers: ($stateParams: ng.ui.IStateParamsService, application: any, GroupService: GroupService) =>
          (application.group && application.group.id &&
          GroupService.getMembers(application.group.id).then(response => response.data))
      },
      data: {
        menu: {
          label: 'Members',
          icon: 'group'
        },
        devMode: true
      }
    })
    .state('management.applications.portal.analytics', {
      url: '/analytics?from&to',
      component: 'applicationAnalytics',
      data: {
        menu: {
          label: 'Analytics',
          icon: 'insert_chart'
        },
        devMode: true
      },
      params: {
        from: {
          type: "int",
          dynamic: true
        },
        to: {
          type: "int",
          dynamic: true
        }
      }
    })
}
