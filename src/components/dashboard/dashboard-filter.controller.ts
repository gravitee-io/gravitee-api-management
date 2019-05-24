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

class DashboardFilterController {
  private fields: any;
  private filters: any[];
  private onFilterChange: any;
  private lastSource: any;

  constructor(private $rootScope, private $state: StateService) {
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
    //init filters based on stateParams
    let q = this.$state.params["q"];
    if (q) {
      this.decodeQueryFilters(q);
    } else {
      this.onFilterChange({query: undefined, widget: this.lastSource});
    }
  }

  private decodeQueryFilters(query) {
    let filters = query.split("AND");
    for (let i = 0; i < filters.length; i++) {
      let queryFilter = filters[i].replace(/[()]/g, "");
      let kv = queryFilter.split(":");
      let k = kv[0].trim();
      let v = kv[1].replace(/[\\\"]/g, "").split('OR').map(x => x.trim());
      
      let filter: any = {};
      filter.key = v;
      filter.name = v
      filter.field = k;
      filter.fieldLabel = k;

      this.addFieldFilter(filter);
    }
  }

  addFieldFilter(filter) {
    let field = this.fields[filter.field] || {filters: {}};

    field.filters[filter.key] = {
      value: filter.name,
      onRemove: (filter.events !== undefined) && filter.events.remove
    };

    let label = (filter.fieldLabel ? filter.fieldLabel : filter.field)
      + " = '" + filter.name + "'";

    let query = '(' + filter.field + ":" + _.map(_.keys(field.filters), (key) => key.includes('TO')?key:"\\\"" + key + "\\\"").join(' OR ') + ')';

    this.filters.push({
      source: filter.widget,
      key: filter.field + '_' + filter.key,
      label: label
    });

    this.filters = _.uniqBy(this.filters, 'key');

    field.query = query;

    this.fields[filter.field] = field;
    this.lastSource = filter.widget;
    this.createAndSendQuery(filter.silent);
  }

  removeFieldFilter(filter) {
    this.removeFilter(filter.field, filter.key, filter.silent);
  }

  deleteChips(chip) {
    let index = chip.key.lastIndexOf('_');
    this.lastSource = chip.source;
    this.removeFilter(chip.key.substring(0, index), chip.key.substring(index+1), false);
  }

  removeFilter(field, key, silent) {
    let filters = _.remove(this.filters, (current) => {
      return current.key === field + '_' + key;
    });

    if (filters.length > 0) {
      this.lastSource = filters[0].source;
    }

    let fieldObject = this.fields[field] || {filters: {}};

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

    if (! _.isEmpty(fieldObject.filters)) {
      fieldObject.query = '(' + field + ":" + _.map(_.keys(fieldObject.filters), (key) => key.includes('TO')?key:"\\\"" + key + "\\\"").join(' OR ') + ')';
      this.fields[field] = fieldObject;
    } else {
      delete this.fields[field];
    }

    this.createAndSendQuery(silent);
  }

  createAndSendQuery(silent) {
    // Create a query with all the current filters
    let query = _.values(_.mapValues(this.fields, function(value) { return value.query; })).join(' AND ');

    // Update the query parameter
    if (!silent) {
      this.$state.transitionTo(
        this.$state.current,
        _.merge(this.$state.params, {
          q: query
        }),
        {notify: false});
      this.onFilterChange({query: query, widget: this.lastSource});
    }
  }
}

export default DashboardFilterController;
