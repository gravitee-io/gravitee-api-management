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
import * as _ from 'lodash';

import { Conditions, Metrics } from '../../../../entities/alert';

const AlertTriggerConditionComponent: ng.IComponentOptions = {
  bindings: {
    condition: '<',
    metrics: '<',
    label: '<',
    isReadonly: '<',
  },
  template: require('./trigger-condition.html'),
  controller: function () {
    'ngInject';

    this.$onInit = () => {
      this.onMetricsChange(false);
    };

    this.onMetricsChange = (reset: boolean) => {
      if (this.metrics) {
        if (reset) {
          delete this.condition.type;
          delete this.condition.operator;
          delete this.condition.multiplier;
          delete this.condition.property2;
        }

        // If no property initialized, takes the first from the select alert
        if (this.condition.property === undefined) {
          this.condition.property = this.metrics[0].key;
        }

        // Get the metric field according to the condition property
        this.conditions = _.find(this.metrics as Metrics[], (metric) => metric.key === this.condition.property).conditions;

        this.onConditionChange();
      }
    };

    this.onConditionChange = () => {
      const condition = Conditions.findByType(this.condition.type);
      if (condition !== undefined) {
        this.operators = condition.getOperators();

        if (this.operators.length === 1) {
          this.condition.operator = this.operators[0].key;
        }
      }
    };
  },
};

export default AlertTriggerConditionComponent;
