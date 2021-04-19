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
import AnalyticsService from '../../services/analytics.service';

class DashboardFilterController {
  private fields: any;
  private filters: any[];
  private onFilterChange: any;
  private lastSource: any;

  constructor(
    private $rootScope,
    private $state: StateService,
    private AnalyticsService: AnalyticsService,
    private $timeout: ng.ITimeoutService,
  ) {
    'ngInject';

    this.fields = {};
    this.filters = [];

    var that = this;
    $rootScope.$on('filterItemChange', function (event, filter) {
      if (filter.mode === 'add') {
        that.addFieldFilter(filter);
      } else if (filter.mode === 'remove') {
        that.removeFieldFilter(filter);
      }
    });
  }

  $onInit() {
    // init filters based on stateParams
    let queryFilters = this.AnalyticsService.getQueryFilters();
    if (queryFilters) {
      this.decodeQueryFilters(queryFilters);
    } else {
      this.onFilterChange({ query: undefined, widget: this.lastSource });
    }
  }

  addFieldFilter(filter, run: boolean = true) {
    let field = this.fields[filter.field] || { filters: {} };

    field.filters[filter.key] = {
      value: filter.name,
      onRemove: filter.events !== undefined && filter.events.remove,
    };

    let label = (filter.fieldLabel ? filter.fieldLabel : filter.field) + " = '" + filter.name + "'";
    let query =
      '(' +
      _.map(_.keys(field.filters), (key) =>
        filter.field.includes('path') || filter.field.includes('host')
          ? filter.field + ':' + '\\"' + key + '\\"'
          : filter.field + ':' + key,
      ).join(' OR ') +
      ')';

    this.filters.push({
      source: filter.widget,
      key: filter.field + '_' + filter.key,
      label: label,
    });

    this.filters = _.uniqBy(this.filters, 'key');

    field.query = query;

    this.fields[filter.field] = field;
    this.lastSource = filter.widget;

    if (run) {
      this.createAndSendQuery(filter.silent);
    }
  }

  removeFieldFilter(filter) {
    this.removeFilter(filter.field, filter.key, filter.silent);
  }

  deleteChips(chip) {
    let index = chip.key.lastIndexOf('_');
    this.lastSource = chip.source;
    this.removeFilter(chip.key.substring(0, index), chip.key.substring(index + 1), false);
  }

  removeFilter(field, key, silent) {
    let filters = _.remove(this.filters, (current) => {
      return current.key === field + '_' + key;
    });

    if (filters.length > 0) {
      this.lastSource = filters[0].source;
    }

    let fieldObject = this.fields[field] || { filters: {} };

    let fieldFilter = fieldObject.filters[key];
    if (fieldFilter) {
      delete fieldObject.filters[key];
      // Is there a registered event ?
      if (fieldFilter.onRemove && !silent) {
        fieldFilter.onRemove(key);
      }
    }

    if (Object.keys(fieldObject.filters).length === 0 || _.isEmpty(fieldObject.filters)) {
      delete fieldObject.filters;
    }

    if (!_.isEmpty(fieldObject.filters)) {
      fieldObject.query =
        '(' +
        _.map(_.keys(fieldObject.filters), (key) =>
          field.includes('path') || field.includes('host') ? field + ':' + '\\"' + key + '\\"' : field + ':' + key,
        ).join(' OR ') +
        ')';
      this.fields[field] = fieldObject;
    } else {
      delete this.fields[field];
    }

    this.createAndSendQuery(silent);
  }

  createAndSendQuery(silent) {
    // Create a query with all the current filters
    let query = Object.keys(this.fields)
      .map((field) => this.fields[field].query)
      .join(' AND ');

    // Update the query parameter
    if (!silent) {
      this.$timeout(async () => {
        await this.$state.transitionTo(
          this.$state.current,
          _.merge(this.$state.params, {
            q: query,
          }),
          { notify: false },
        );
        this.onFilterChange({ query: query, widget: this.lastSource });
      });
    }
  }

  private decodeQueryFilters(queryFilters) {
    let filters = Object.keys(queryFilters);
    let lastFilter;
    filters.forEach((filter) => {
      let k = filter;
      let v = queryFilters[filter];

      v.forEach((value) => {
        let filter: any = {};
        filter.key = value;
        filter.name = value;
        filter.field = k;
        filter.fieldLabel = k;
        this.addFieldFilter(filter, false);
        lastFilter = filter;
      });
    });
    this.createAndSendQuery(lastFilter.silent);
  }
}

export default DashboardFilterController;
