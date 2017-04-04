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
import SidenavService from './sidenav.service';

export const SubmenuComponent: ng.IComponentOptions = {
  template: require('./submenu.html'),
  bindings: {
    allMenuItems: '<',
    reducedMode: '<'
  },
  require: {
    parent: '^gvSidenav'
  },
  controller: function(SidenavService: SidenavService, $filter: ng.IFilterService, $transitions) {
    'ngInject';

    this.sidenavService = SidenavService;

    let that = this;
    $transitions.onSuccess({ }, function() {
      that.reload();
    });

    this.$onInit = function() {
      that.reload();
    };

    this.reload = function() {
      that.submenuItems = $filter<any>('currentSubmenus')(that.allMenuItems);
    };
  }
};
