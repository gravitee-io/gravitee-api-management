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

import { IScope } from 'angular';
import RoleService from '../../services/role.service';
import '@gravitee/ui-components/wc/gv-switch';

interface IPageScope extends IScope {
  fetcherJsonSchema: string;
  rename: boolean;
  editorReadonly: boolean;
  currentTab: string;
  currentTranslation: any;
  acls: any;
}

const EditPageAclsComponent: ng.IComponentOptions = {
  bindings: {
    page: '<',
    groups: '<',
    roles: '<'
  },
  template: require('./edit-page-acls.html'),
  controller: function(
    $scope: IPageScope,
    RoleService: RoleService,
  ) {
    'ngInject';

    this.$onInit = () => {

      RoleService.list('ENVIRONMENT').then((roles) => {
        this.roles = roles;
      });

      $scope.acls = {
        isPrivate: this.page.visibility.toLowerCase() === 'private',
        groups: this.page.accessControls?.filter((acl) => acl.referenceType.toUpperCase() === 'GROUP').map((group) => group.referenceId),
        roles: this.page.accessControls?.filter((acl) => acl.referenceType.toUpperCase() === 'ROLE').map((role) => role.referenceId),
        excludedAccessControls: this.page.excludedAccessControls
      };

      $scope.$watch('acls', () => {
        this.page.visibility = $scope.acls.isPrivate ? 'private' : 'public';
        this.page.excludedAccessControls = $scope.acls.excludedAccessControls;

        this.page.accessControls = [
          $scope.acls.groups?.map((acl) => ({referenceId: acl, referenceType: 'GROUP'})),
          $scope.acls.roles?.map((acl) => ({referenceId: acl, referenceType: 'ROLE'})),
        ]
          .filter((acl) => acl != null)
          .reduce((acc, val) => acc.concat(val), []);

      }, true);

    };

  }

};

export default EditPageAclsComponent;
