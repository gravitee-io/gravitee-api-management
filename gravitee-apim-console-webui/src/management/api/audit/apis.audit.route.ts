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
import AuditService from '../../../services/audit.service';
import { ApimFeature } from '../../../shared/components/gio-license/gio-license-data';

export default apisAuditRouterConfig;

/* @ngInject */
function apisAuditRouterConfig($stateProvider) {
  $stateProvider
    .state('management.apis.detail.audit', {
      abstract: true,
    })
    .state('management.apis.detail.audit.general', {
      url: '/audit',
      template: require('./general/audit.html'),
      controller: 'ApiAuditController',
      controllerAs: 'auditCtrl',
      data: {
        requireLicense: {
          license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
          redirect: 'management.apis.ng-list',
        },
        perms: {
          only: ['api-audit-r'],
        },
        docs: {
          page: 'management-api-audit',
        },
      },
      resolve: {
        resolvedEvents: (AuditService: AuditService, $stateParams) =>
          AuditService.listEvents($stateParams.apiId).then((response) => response.data),
      },
    })
    .state('management.apis.detail.audit.history', {
      url: '/history',
      template: require('./history/apiHistory.html'),
      controller: 'ApiHistoryController',
      controllerAs: 'apiHistoryCtrl',
      data: {
        perms: {
          only: ['api-event-r'],
        },
        docs: {
          page: 'management-api-history',
        },
      },
    })
    .state('management.apis.detail.audit.events', {
      url: '/events',
      template: require('./events/apiEvents.html'),
      controller: 'ApiEventsController',
      controllerAs: 'apiEventsCtrl',
      data: {
        perms: {
          only: ['api-event-r'],
        },
        docs: {
          page: 'management-api-events',
        },
      },
    });
}
