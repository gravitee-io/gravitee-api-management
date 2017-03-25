"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var DashboardFilterController = (function () {
    function DashboardFilterController($rootScope) {
        'ngInject';
        this.$rootScope = $rootScope;
        this.fields = {};
        this.filters = [];
        var that = this;
        $rootScope.$on('filterItemChange', function (event, filter) {
            if (filter.mode === 'add') {
                that.addFieldFilter(filter);
            }
            else if (filter.mode === 'remove') {
                that.removeFieldFilter(filter);
            }
        });
    }
    DashboardFilterController.prototype.addFieldFilter = function (filter) {
        var field = this.fields[filter.field] || { filters: {} };
        field.filters[filter.key] = filter.name;
        var label = filter.field + " = '" + filter.name + "'";
        var query = '(' + _.map(_.keys(field.filters), function (key) { return filter.field + ":" + key; }).join(' OR ') + ')';
        this.filters.push({
            key: filter.field + '_' + filter.key,
            label: label
        });
        field.query = query;
        this.fields[filter.field] = field;
        this.createAndSendQuery();
    };
    DashboardFilterController.prototype.removeFieldFilter = function (filter) {
        this.removeFilter(filter.field, filter.key);
    };
    DashboardFilterController.prototype.deleteChips = function (event) {
        var parts = event.key.split('_');
        this.removeFilter(parts[0], parts[1]);
    };
    DashboardFilterController.prototype.removeFilter = function (field, key) {
        _.remove(this.filters, function (current) {
            return current.key === field + '_' + key;
        });
        var fieldObject = this.fields[field] || { filters: {} };
        delete fieldObject.filters[key];
        if (Object.keys(fieldObject.filters).length === 0 || _.isEmpty(fieldObject.filters)) {
            delete fieldObject.filters;
        }
        if (!_.isEmpty(fieldObject.filters)) {
            fieldObject.query = '(' + _.map(_.keys(fieldObject.filters), function (key) { return field + ":" + key; }).join(' OR ') + ')';
            this.fields[field] = fieldObject;
        }
        else {
            delete this.fields[field];
        }
        this.createAndSendQuery();
    };
    DashboardFilterController.prototype.createAndSendQuery = function () {
        // Create a query with all the current filters
        var query = _.values(_.mapValues(this.fields, function (value) { return value.query; })).join(' AND ');
        this.onFilterChange({ query: query });
    };
    return DashboardFilterController;
}());
exports.default = DashboardFilterController;
