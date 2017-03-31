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

import PortalPagesService from "../../services/portalPages.service";

const PageComponent: ng.IComponentOptions = {
  bindings: {
    page: '<'
  },
  template: require('./page.html'),
  controller: function(PortalPagesService: PortalPagesService) {
    'ngInject';

    this.$onInit = function() {
      PortalPagesService.cachePageConfiguration(this.page);
    };
  }
};

export default PageComponent;
