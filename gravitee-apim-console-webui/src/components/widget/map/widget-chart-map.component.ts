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
// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-map');
const WidgetChartMapComponent: ng.IComponentOptions = {
  template: require('./widget-chart-map.html'),
  bindings: {
    data: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: function widgetChartMapController($scope, $element) {
    'ngInject';

    this.options = {
      name: 'Number of API requests',
      excludedKeys: ['Unknown'],
    };

    this.$onInit = () => {
      $scope.$on('onWidgetResize', this.onResize.bind(this));
    };

    this.$onChanges = function (changes) {
      this.gvChartMap = $element.children()[0];

      if (changes.data) {
        const series = {
          values: changes.data.currentValue ? changes.data.currentValue.values : [],
        };

        // Send data to gv-chart-map
        this.gvChartMap.setAttribute('series', JSON.stringify(series));
        this.gvChartMap.setAttribute('options', JSON.stringify(this.options));
      }
    };

    this.onResize = () => {
      this.options = {
        ...this.options,
        height: this.gvChartMap.offsetHeight,
        width: this.gvChartMap.offsetWidth,
      };
      this.gvChartMap.setAttribute('options', JSON.stringify(this.options));
    };
  },
};

export default WidgetChartMapComponent;
