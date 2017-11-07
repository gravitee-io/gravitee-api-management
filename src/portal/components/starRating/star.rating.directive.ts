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
import _ = require('lodash');

const StarRatingDirective: ng.IDirective = ({
  restrict: 'EA',
  scope: {
    'value': '=?value',
    'summary': '=summary',
    'isBigSize': '=isBigSize',
    'message': '=message',
    'displayNoneMessage': '=displayNoneMessage',
    'isReadonly': '=isReadonly'
  },
  link: function (scope) {
    if (scope.summary) {
      scope.value = scope.summary.averageRate;
    }

    scope.initialValue = _.clone(scope.value);

    function renderValue() {
      scope.starsValue = [];
      for (let i = 0; i < 5; i++) {
        if (i < scope.value) {
          scope.starsValue.push(true);
        } else {
          scope.starsValue.push(false);
        }
      }
    }

    scope.setValue = function (index) {
      if (!scope.isReadonly) {
        scope.value = index;
      }
    };

    scope.selectValue = function (index) {
      if (!scope.isReadonly) {
        scope.setValue(index);
        scope.initialValue = _.clone(index);
      }
    };

    scope.$watch('value', function (newValue: number) {
      if (newValue === undefined) {
        scope.initialValue = 0;
      }
      renderValue();
      scope.stylePos = Math.round((newValue * 10) / 5) * 5;
    });
  },
  template: require('./star.rating.directive.html'),
  replace: true
});

export default StarRatingDirective;
