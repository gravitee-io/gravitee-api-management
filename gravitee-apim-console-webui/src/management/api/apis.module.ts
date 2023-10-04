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

import { NgModule } from '@angular/core';
import { UIRouterModule } from '@uirouter/angular';
import { Transition, TransitionService } from '@uirouter/angularjs';
import * as angular from 'angular';
import { switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

import { ApiAnalyticsModule } from './analytics/api-analytics.module';
import { ApiListModule } from './list/api-list.module';
import { ApiNavigationModule } from './api-navigation/api-navigation.module';
import { ApiProxyModule } from './proxy/api-proxy.module';
import { ApiV4PolicyStudioModule } from './policy-studio-v4/api-v4-policy-studio.module';
import { ApiRuntimeLogsV4Module } from './runtime-logs-v4/api-runtime-logs-v4.module';
import { ApisGeneralModule } from './general/apis-general.module';
import { ApiEndpointsModule } from './endpoints-v4/api-endpoints.module';
import { ApiEntrypointsV4Module } from './entrypoints-v4/api-entrypoints-v4.module';
import { GioPolicyStudioRoutingModule } from './policy-studio/gio-policy-studio-routing.module';
import { ApiAuditModule } from './audit/api-audit.module';
import { ApiV1PoliciesComponent } from './design/policies/policies.component';
import { states } from './apis.route';
import { NotificationsListModule } from './notifications/notifications-list/notifications-list.module';
import { ApiCreationV2Module } from './creation-v2/api-creation-v2.module';
import { ApiCreationGetStartedModule } from './creation-get-started/api-creation-get-started.module';
import { ApiCreationV4Module } from './creation-v4/api-creation-v4.module';
import { NotificationDetailsModule } from './notifications/notifications-list/notofication-details/notification-details.module';
import { ApiDocumentationV4Module } from './documentation-v4/api-documentation-v4.module';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { GioEmptyModule } from '../../shared/components/gio-empty/gio-empty.module';
import { SpecificJsonSchemaTypeModule } from '../../shared/components/specific-json-schema-type/specific-json-schema-type.module';
import { DocumentationModule } from '../../components/documentation/documentation.module';

const graviteeManagementModule = angular.module('gravitee-management');
apiPermissionHook.$inject = ['$transitions', 'ngGioPermissionService'];
function apiPermissionHook($transitions: TransitionService, gioPermissionService: GioPermissionService) {
  $transitions.onBefore(
    {
      to: 'management.apis.**',
    },
    (transition: Transition) => {
      const stateService = transition.router.stateService;

      return gioPermissionService
        .loadApiPermissions(transition.params().apiId)
        .pipe(
          switchMap(() => {
            const permissions = transition.$to().data?.apiPermissions?.only;
            if (!permissions) {
              return of(true);
            }
            if (gioPermissionService.hasAnyMatching(permissions)) {
              return of(true);
            }
            return of(stateService.target('login'));
          }),
        )
        .toPromise();
    },
    { priority: 9 },
  );
}
graviteeManagementModule.run(apiPermissionHook);

@NgModule({
  declarations: [ApiV1PoliciesComponent],
  imports: [
    ApiAnalyticsModule,
    ApiListModule,
    ApiNavigationModule,
    ApiV4PolicyStudioModule,
    ApiRuntimeLogsV4Module,
    ApisGeneralModule,
    ApiProxyModule,
    ApiEntrypointsV4Module,
    ApiEndpointsModule,
    ApiAuditModule,
    NotificationsListModule,
    NotificationDetailsModule,
    GioPolicyStudioRoutingModule.withRouting({ stateNamePrefix: 'management.apis.policy-studio-v2' }),
    SpecificJsonSchemaTypeModule,
    DocumentationModule,
    ApiCreationGetStartedModule,
    ApiCreationV2Module,
    ApiCreationV4Module,
    ApiDocumentationV4Module,

    GioEmptyModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
