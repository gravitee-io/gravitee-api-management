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
import RoleService from "../../../services/role.service";

export default apisMessagesRouterConfig;

function apisMessagesRouterConfig($stateProvider) {
  'ngInject';
  $stateProvider
    .state('management.apis.detail.messages', {
      url: '/messages',
      component: 'messages',
      resolve: {
        resolvedScope: () => "APPLICATION",
        resolvedApiId: ($stateParams) => $stateParams.apiId,
        resolvedRoles: (RoleService: RoleService) => RoleService.list("APPLICATION")
      },
      data: {
        menu: {
          label: 'Messages',
          icon: 'message'
        },
        perms: {
          only: ['api-message-c']
        },
        docs: {
          page: 'management-messages'
        }
      }
    })
}
