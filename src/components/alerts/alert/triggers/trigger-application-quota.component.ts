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

import { ApiMetrics } from '../../../../entities/alerts/api.metrics';

const AlertTriggerApplicationQuotaComponent: ng.IComponentOptions = {
  bindings: {
    alert: '<',
  },
  require: {
    parent: '^alertComponent',
  },
  template: require('./trigger-application-quota.html'),
  controller: function () {
    'ngInject';

    this.$onInit = () => {
      this.metrics = [ApiMetrics.QUOTA_COUNTER];

      // New alert, initialize it with the condition model
      if (this.alert.id === undefined) {
        this.alert.conditions = [
          {
            type: 'COMPARE',
            operator: 'GTE',
            property: 'quota.counter',
            property2: 'quota.limit',
          },
        ];

        this.alert.dampening = {
          mode: 'STRICT_COUNT',
          trueEvaluations: 1,
          totalEvaluations: 1,
        };
      }

      this.threshold = this.alert.conditions[0].multiplier * 100;
    };

    this.calculateMultiplier = () => {
      this.alert.conditions[0].multiplier = this.threshold / 100;
    };
  },
};

export default AlertTriggerApplicationQuotaComponent;
