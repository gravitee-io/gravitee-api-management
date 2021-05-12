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

import { Metrics, RateCondition } from '../../../../entities/alert';
import { Rule } from '../../../../entities/alerts/rule.metrics';

const AlertTriggerMetricsRateComponent: ng.IComponentOptions = {
  bindings: {
    alert: '<',
  },
  require: {
    parent: '^alertComponent',
  },
  template: require('./trigger-metrics-rate.html'),
  controller: function () {
    'ngInject';

    this.$onInit = () => {
      this.metrics = Metrics.filterByScope(
        Rule.findByScopeAndType(this.alert.reference_type, this.alert.type).metrics,
        this.alert.reference_type,
      );
      this.operators = RateCondition.OPERATORS;

      // New alert, initialize it with the condition model
      if (this.alert.id === undefined) {
        this.alert.conditions = [
          {
            operator: 'GT',
            type: 'RATE',
            comparison: {
              property: this.metrics[0].key,
              operator: 'GT',
              threshold: 100.0,
              type: 'THRESHOLD',
            },
          },
        ];

        this.alert.dampening = {
          mode: 'STRICT_COUNT',
          trueEvaluations: 1,
          totalEvaluations: 1,
        };
      }
    };
  },
};

export default AlertTriggerMetricsRateComponent;
