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
class WidgetChartTableDirective {
  constructor() {
    this.restrict = 'E';
    this.scope = {
      data: "=data"
    };
    this.templateUrl = 'app/components/widget/widget-table.html';
  }

  controller($scope) {
    this.$scope = $scope;
    this.$scope.selected = [];

    $scope.selectItem = function(item) {
      $scope.updateQuery(item, true);
    };

    $scope.deselectItem = function(item) {
      $scope.updateQuery(item, false);
    };

    $scope.updateQuery = function(item, add) {
      $scope.$emit('filterItemChange', {
        widget: $scope.$parent.$parent.$parent.widget.$uid,
        field: $scope.$parent.chart.request.field,
        key: item.key,
        name: item.metadata.name,
        mode: (add) ? 'add' : 'remove'
      });
    };
  }

  link(scope) {
    scope.$watch('data', function(data) {
      if (data) {
        scope.paging = 1;
        scope.results = _.map(data.values, function (value, key) {
          return {
            key: key,
            value: value,
            metadata: (data && data.metadata) ? data.metadata[key] : undefined
          };
        });
      }
    }, true);
  }
}

export default WidgetChartTableDirective;
