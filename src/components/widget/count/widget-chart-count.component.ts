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
require('@gravitee/ui-components/wc/gv-card-full');
const WidgetChartCountComponent: ng.IComponentOptions = {
  template: require('./widget-chart-count.html'),
  bindings: {
    data: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: function () {
    this.$onChanges = function (changes) {
      if (changes.data && changes.data.currentValue) {
        this.count = changes.data.currentValue.count || 0;
      }
    };
  },
};

export default WidgetChartCountComponent;
