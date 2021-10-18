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
const WidgetDataStatsComponent: ng.IComponentOptions = {
  template: require('./widget-data-stats.html'),
  bindings: {
    data: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: function ($scope, $window, $filter) {
    'ngInject';
    this.$onInit = () => {
      this.chartData = _.cloneDeep(this.parent.widget.chart.data);
      checkFallback();
      checkResolution();
      $scope.$on('onWidgetResize', () => {
        checkResolution();
      });
    };
    const checkFallback = () => {
      _.forEach(this.chartData, (data) => {
        let hasFallback = false;
        let value = this.data[data.key];
        if ($filter('number')(value, 0) === '0' && data.fallback && data.fallback.length) {
          _.forEach(data.fallback, (fallback) => {
            let fallbackValue = this.data[fallback.key];
            if (!hasFallback && $filter('number')(fallbackValue, 0) !== '0') {
              data.key = fallback.key;
              data.label = fallback.label;
              hasFallback = true;
            }
          });
        }
      });
    };
    const checkResolution = () => {
      this.lowResolution = $window.innerWidth < 1400;
    };
  },
};

export default WidgetDataStatsComponent;
