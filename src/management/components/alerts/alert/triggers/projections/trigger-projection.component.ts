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
import {ApiMetrics} from "../../../../../../entities/alerts/api.metrics";
import {NodeLifecycleMetrics, NodeMetrics} from "../../../../../../entities/alerts/node.metrics";
import {HealthcheckMetrics} from "../../../../../../entities/alerts/healthcheck.metrics";
import {Metrics} from "../../../../../../entities/alert";
import _ = require('lodash');

const AlertTriggerProjectionComponent: ng.IComponentOptions = {
  bindings: {
    projection: '<',
    alert: '<',
    onProjectionRemove: '&'
  },
  template: require('./trigger-projection.html'),
  controller: function () {
    'ngInject';

    this.$onInit = () => {
      // Metrics are depending on the source of the trigger
      if (this.alert.source === 'REQUEST') {
        this.metrics = Metrics.filterByScope(ApiMetrics.METRICS, this.alert.reference_type);
      } else if(this.alert.source === 'NODE_HEARTBEAT') {
        this.metrics = Metrics.filterByScope(NodeMetrics.METRICS, this.alert.reference_type);
      } else if(this.alert.source === 'ENDPOINT_HEALTH_CHECK') {
        this.metrics = Metrics.filterByScope(HealthcheckMetrics.METRICS, this.alert.reference_type);
      } else if(this.alert.source === 'NODE_LIFECYCLE') {
        this.metrics = Metrics.filterByScope(NodeLifecycleMetrics.METRICS, this.alert.reference_type);
      }

      this.metrics = _.filter(this.metrics, (metric) => { return metric.supportPropertyProjection; })
    };

    this.deleteProjection = () => {
      this.onProjectionRemove();
    }
  }
};

export default AlertTriggerProjectionComponent;
