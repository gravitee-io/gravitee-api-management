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
import { cloneDeep } from 'lodash';
// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-stats');
const WidgetDataStatsComponent: ng.IComponentOptions = {
  template: require('html-loader!./widget-data-stats.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    data: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: [
    '$scope',
    '$element',
    function ($scope, $element) {
      this.$onInit = () => {
        this.chartData = cloneDeep(this.parent.widget.chart.data);
        checkFallback();
      };
      const checkFallback = () => {
        const gvStats = $element.children()[0];

        const stats = {};
        if (Object.values(this.data).some(data => data !== 0)) {
          this.chartData.forEach(data => {
            stats[data.key] = this.data[data.key];
          });
        }

        const options = this.chartData.map(data => {
          return {
            key: data.key,
            unit: data.unit,
            color: data.color,
            label: data.label,
            fallback:
              data.fallback &&
              data.fallback.map(fallback => {
                return { key: fallback.key, label: fallback.label };
              }),
          };
        });

        // Send data to gv-stats
        gvStats.setAttribute('stats', JSON.stringify(stats));
        gvStats.setAttribute('options', JSON.stringify(options));
      };
    },
  ],
};

export default WidgetDataStatsComponent;
