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
const AlertTriggerFiltersComponent: ng.IComponentOptions = {
  bindings: {
    alert: '<',
    form: '<',
    isReadonly: '<',
  },
  template: require('./trigger-filters.html'),
  controller: function () {
    'ngInject';

    this.addFilter = () => {
      if (this.alert.filters === undefined) {
        this.alert.filters = [];
      }

      this.alert.filters.push({});
    };

    this.removeFilter = (idx: number) => {
      this.alert.filters.splice(idx, 1);
      this.form.$setDirty();
    };
  },
};

export default AlertTriggerFiltersComponent;
