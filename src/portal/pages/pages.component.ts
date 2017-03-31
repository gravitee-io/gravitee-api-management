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
const PagesComponent: ng.IComponentOptions = {
  bindings: {
    pages: '<'
  },
  template: require('./pages.html'),
  controller: function($stateParams, $location) {
    'ngInject';

    this.$onInit = function() {
      if (this.pages.length && !$stateParams.pageId) {
        $location.url(`/pages/${this.pages[0].id}`);
      }
    };
  }
};

export default PagesComponent;
