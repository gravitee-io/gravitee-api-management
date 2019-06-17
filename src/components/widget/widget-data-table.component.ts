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
import { StateService } from '@uirouter/core';

const WidgetDataTableComponent: ng.IComponentOptions = {
  template: require('./widget-data-table.html'),
  bindings: {
    data: '<'
  },
  require: {
    parent: '^gvWidget'
  },
  controller: function($scope, $state: StateService) {
    'ngInject';

    this.$scope = $scope;
    this.$state = $state;
    this.selected = [];

    this.$onInit = function() {
      this.widget = this.parent.widget;
    };

    this.$onChanges = function(changes) {
      if (changes.data) {
        let data = changes.data.currentValue;
        this.paging = 1;
        this.results = _.map(data.values, function (value, key) {
          return {
            key: key,
            value: value,
            metadata: (data && data.metadata) ? data.metadata[key] : undefined
          };
        });
      }
    };

    let that = this;
    this.selectItem = function (item) {
      that.updateQuery(item, true);
    };
    this.deselectItem = function (item) {
      that.updateQuery(item, false);
    };

    this.updateQuery = function(item, add) {
      that.$scope.$emit('filterItemChange', {
        widget: that.widget.$uid,
        field: that.widget.chart.request.field,
        fieldLabel: that.widget.chart.request.fieldLabel,
        key: item.key,
        name: item.metadata.name,
        mode: (add) ? 'add' : 'remove'
      });
    };

    this.goto = function(key) {
      if (this.widget.chart.link === 'api') {
        return this.$state.href('management.apis.detail.analytics.overview', {apiId: key, from: this.widget.chart.request.from, to: this.widget.chart.request.to, q:this.widget.chart.request.query});
      } else if (this.widget.chart.link === 'application') {
        return this.$state.href('management.applications.application.analytics', {applicationId: key, from: this.widget.chart.request.from, to: this.widget.chart.request.to, q:this.widget.chart.request.query});
      }
    }
  }
};

export default WidgetDataTableComponent;
