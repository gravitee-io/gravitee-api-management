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
import { IController, IScope } from 'angular';

import RoleService from '../../../services/role.service';
import '@gravitee/ui-components/wc/gv-switch';

interface IPageScope extends IScope {
  acls: any;
}

class EditPageAclsComponentController implements IController {
  page: any;
  groups: any[];
  roles: any[];
  isApiPage: boolean;

  constructor(
    private readonly RoleService: RoleService,
    private $scope: IPageScope,
  ) {}

  $onChanges() {
    const scope = this.isApiPage ? 'API' : 'ENVIRONMENT';
    this.RoleService.list(scope).then((roles) => {
      this.roles = roles;
    });

    this.$scope.acls = {
      isPrivate: this.page.visibility === 'PRIVATE',
      groups: this.page.accessControls?.filter((acl) => acl.referenceType === 'GROUP').map((group) => group.referenceId),
      roles: this.page.accessControls?.filter((acl) => acl.referenceType === 'ROLE').map((role) => role.referenceId),
      excludedAccessControls: this.page.excludedAccessControls,
    };

    this.$scope.$watch(
      'acls',
      () => {
        this.page.visibility = this.$scope.acls.isPrivate ? 'PRIVATE' : 'PUBLIC';
        this.page.excludedAccessControls = this.$scope.acls.excludedAccessControls;

        this.page.accessControls = [
          this.$scope.acls.groups?.map((acl) => ({ referenceId: acl, referenceType: 'GROUP' })),
          this.$scope.acls.roles?.map((acl) => ({ referenceId: acl, referenceType: 'ROLE' })),
        ]
          .filter((acl) => acl != null)
          .reduce((acc, val) => acc.concat(val), []);
      },
      true,
    );
  }
}
EditPageAclsComponentController.$inject = ['RoleService', '$scope'];

export const EditPageAclsComponent: ng.IComponentOptions = {
  bindings: {
    page: '<',
    groups: '<',
    roles: '<',
    isApiPage: '<',
  },
  template: require('html-loader!./edit-page-acls.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: EditPageAclsComponentController,
};
