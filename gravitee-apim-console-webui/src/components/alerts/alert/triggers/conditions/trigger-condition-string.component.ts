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

import { find } from 'lodash';

import { Metrics } from '../../../../../entities/alert';

const AlertTriggerConditionStringComponent: ng.IComponentOptions = {
  bindings: {
    condition: '<',
    metrics: '<',
    isReadonly: '<',
    referenceType: '<',
    referenceId: '<',
  },
  template: require('html-loader!./trigger-condition-string.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    '$injector',
    function ($injector) {
      this.$onInit = async () => {
        const metric = find(this.metrics as Metrics[], metric => metric.key === this.condition.property);

        if (metric.loader) {
          this.values = metric.loader(this.referenceType, this.referenceId, $injector);
          this.filteredValues = this.values; // initialize filtered list
        }

        this.searchTerm = '';
      };

      this.onSearch = () => {
        if (!this.searchTerm || this.searchTerm.trim() === '') {
          this.filteredValues = this.values;
        } else {
          const term = this.searchTerm.toLowerCase();
          this.filteredValues = this.values.filter(item => item.value.toLowerCase().includes(term));
        }
      };

      this.displaySelect = () => {
        return this.values !== undefined && (this.condition.operator === 'EQUALS' || this.condition.operator === 'NOT_EQUALS');
      };
    },
  ],
};

export default AlertTriggerConditionStringComponent;
