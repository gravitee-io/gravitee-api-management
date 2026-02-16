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
import { Router } from '@angular/router';
import { includes, map } from 'lodash';

import AnalyticsService from '../../../../services/analytics.service';
import { Constants } from '../../../../entities/Constants';

const WidgetDataTableComponent: ng.IComponentOptions = {
  template: require('html-loader!./widget-data-table.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    data: '<',
    activatedRoute: '<',
  },
  require: {
    parent: '^gvWidget',
  },
  controller: [
    'Constants',
    '$scope',
    'AnalyticsService',
    'ngRouter',
    function (constants: Constants, $scope, AnalyticsService: AnalyticsService, ngRouter: Router) {
      this.constants = constants;
      this.$scope = $scope;
      this.AnalyticsService = AnalyticsService;
      this.ngRouter = ngRouter;
      this.selected = [];

      this.$onInit = function () {
        this.widget = this.parent.widget;
      };

      this.$onChanges = changes => {
        if (changes.data) {
          const data = changes.data.currentValue;
          this.paging = 1;
          this.results = map(data.values, (value, key) => {
            let percent;
            if (includes(value, '/')) {
              const splittedValue = value.split('/');
              value = parseInt(splittedValue[0], 10);
              percent = parseFloat(splittedValue[1]);
            }
            const result = {
              key: key,
              value: value,
              percent: percent,
              metadata: data && data.metadata ? { ...data.metadata[key], order: +data.metadata[key].order } : undefined,
            };
            const widget = this.widget || this.parent.widget;
            if (widget) {
              const queryFilters = this.AnalyticsService.getQueryFilters(this.activatedRoute);
              if (queryFilters) {
                const queryFilter = queryFilters[widget.chart.request.field];
                if (queryFilter && queryFilter.includes(key)) {
                  setTimeout(() => {
                    this.selected.push(result);
                  });
                }
              }
            }
            return result;
          });
        }
      };

      this.selectItem = item => {
        this.updateQuery(item, true);
      };

      this.deselectItem = item => {
        this.updateQuery(item, false);
      };

      this.updateQuery = (item, add) => {
        this.$scope.$emit('filterItemChange', {
          widget: this.widget.$uid,
          field: this.widget.chart.request.field,
          fieldLabel: this.widget.chart.request.fieldLabel,
          key: item.key,
          name: item.metadata.name,
          mode: add ? 'add' : 'remove',
        });
      };

      this.isClickable = function (result) {
        return (
          this.ngRouter.url.includes('/dashboard') &&
          !result.metadata.unknown &&
          (this.widget.chart.request.field === 'api' || this.widget.chart.request.field === 'application')
        );
      };

      this.goto = function (key) {
        // only on platform analytics
        if (this.ngRouter.url.includes('/dashboard')) {
          if (this.widget.chart.request.field === 'api') {
            this.ngRouter.navigate([this.constants.org.currentEnv.id, 'apis', key, 'v2', 'analytics-overview'], {
              queryParams: { from: this.widget.chart.request.from, to: this.widget.chart.request.to },
            });
          } else if (this.widget.chart.request.field === 'application') {
            this.ngRouter.navigate([this.constants.org.currentEnv.id, 'applications', key, 'analytics'], {
              queryParams: { from: this.widget.chart.request.from, to: this.widget.chart.request.to },
            });
          }
        }
      };
    },
  ],
};

export default WidgetDataTableComponent;
