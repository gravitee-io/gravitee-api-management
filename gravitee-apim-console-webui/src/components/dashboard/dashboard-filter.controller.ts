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
import { IOnDestroy, IOnInit, IRootScopeService } from 'angular';
import { ActivatedRoute, Router } from '@angular/router';
import { isEmpty, keys, map, remove, uniqBy } from 'lodash';

import AnalyticsService from '../../services/analytics.service';

class DashboardFilterController implements IOnInit, IOnDestroy {
  private readonly fields: any;
  private filters: any[];
  private onFilterChange: any;
  private lastSource: any;
  private readonly filterItemChangeListener: () => void;
  private activatedRoute: ActivatedRoute;

  constructor(
    private readonly $rootScope: IRootScopeService,
    private readonly ngRouter: Router,
    private readonly AnalyticsService: AnalyticsService,
    private readonly $timeout: ng.ITimeoutService,
  ) {
    this.fields = {};
    this.filters = [];

    this.filterItemChangeListener = $rootScope.$on('filterItemChange', (event, filter) => {
      if (filter.mode === 'add') {
        this.addFieldFilter(filter);
      } else if (filter.mode === 'remove') {
        this.removeFieldFilter(filter);
      }
    });
  }

  $onDestroy(): void {
    this.filterItemChangeListener();
  }

  $onInit() {
    // init filters based on stateParams
    const queryFilters = this.AnalyticsService.getQueryFilters(this.activatedRoute);
    if (queryFilters) {
      this.decodeQueryFilters(queryFilters);
    } else {
      this.onFilterChange({ query: undefined, widget: this.lastSource });
    }
  }

  addFieldFilter(filter, run = true) {
    const field = this.fields[filter.field] || { filters: {} };

    field.filters[filter.key] = {
      value: filter.name,
      onRemove: filter.events !== undefined && filter.events.remove,
    };

    const label = (filter.fieldLabel ? filter.fieldLabel : filter.field) + " = '" + filter.name + "'";
    const query =
      '(' +
      map(keys(field.filters), (key) =>
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

    this.filters = uniqBy(this.filters, 'key');

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
    const index = chip.key.lastIndexOf('_');
    this.lastSource = chip.source;
    this.removeFilter(chip.key.substring(0, index), chip.key.substring(index + 1), false);
  }

  removeFilter(field, key, silent) {
    const filters = remove(this.filters, (current) => {
      return current.key === field + '_' + key;
    });

    if (filters.length > 0) {
      this.lastSource = filters[0].source;
    }

    const fieldObject = this.fields[field] || { filters: {} };

    const fieldFilter = fieldObject.filters[key];
    if (fieldFilter) {
      delete fieldObject.filters[key];
      // Is there a registered event ?
      if (fieldFilter.onRemove && !silent) {
        fieldFilter.onRemove(key);
      }
    }

    if (Object.keys(fieldObject.filters).length === 0 || isEmpty(fieldObject.filters)) {
      delete fieldObject.filters;
    }

    if (!isEmpty(fieldObject.filters)) {
      fieldObject.query =
        '(' +
        map(keys(fieldObject.filters), (key) =>
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
    const query = Object.keys(this.fields)
      .map((field) => this.fields[field].query)
      .join(' AND ');

    // Update the query parameter
    if (!silent) {
      this.$timeout(async () => {
        await this.ngRouter.navigate(['.'], {
          relativeTo: this.activatedRoute,
          queryParams: {
            ...this.activatedRoute.snapshot.queryParams,
            q: query,
          },
        });

        this.onFilterChange({ query: query, widget: this.lastSource });
      });
    }
  }

  private decodeQueryFilters(queryFilters) {
    const filters = Object.keys(queryFilters);
    let lastFilter;
    filters.forEach((filter) => {
      const k = filter;
      const v = queryFilters[filter];

      v.forEach((value) => {
        const filter: any = {};
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
DashboardFilterController.$inject = ['$rootScope', 'ngRouter', 'AnalyticsService', '$timeout'];

export default DashboardFilterController;
