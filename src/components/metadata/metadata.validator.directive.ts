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

import {IScope, IDirective} from 'angular';

interface IMyScope extends IScope {
  ngPattern: RegExp;
}

const MetadataValidatorDirective: IDirective = ({
  restrict: 'A',
  require: 'ngModel',
  scope: {
    format: '=gvMetadataFormat',
    ngPattern: '='
  },
  link: function (scope: IMyScope) {
    scope.$watch('format', function (newFormat) {
      switch (newFormat) {
        case 'numeric':
          scope.ngPattern = /^((\$\{.+\})|\d+(\.\d{1,2})?)$/;
          break;
        case 'url':
          scope.ngPattern = /^((\$\{.+\})|(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?)$/;
          break;
        case 'mail':
          scope.ngPattern = /^((\$\{.+\})|(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))$/;
          break;
        case 'string':
          delete scope.ngPattern;
          break;
      }
    });
  }
});

export default MetadataValidatorDirective;
