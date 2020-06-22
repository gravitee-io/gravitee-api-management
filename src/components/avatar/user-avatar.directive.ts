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
class UserAvatarDirective {

  constructor() {
    'ngInject';

    let directive = {
      restrict: 'A',
      controller: UserAvatarController,
      link: function (scope, element, attrs, ctrl) {
        attrs.$observe('ngSrc', function () {
          if (attrs.graviteeUserAvatar) {
            var deferred = ctrl.$q.defer();
            var image = new Image();
            image.onerror = function () {
              deferred.resolve(false);
              // Default image
              element.attr('src', 'assets/default_photo.png');
            };
            image.onload = function () {
              deferred.resolve(true);
            };
            image.src = ctrl.Constants.orgBaseURL + '/users/' + attrs.graviteeUserAvatar + '/avatar';
            return deferred.promise;
          } else {
            // Default image
            element.attr('src', 'assets/default_photo.png');
          }
        });
      }
    };

    return directive;
  }
}

class UserAvatarController {
  constructor(private $scope, private $q, private Constants) {
    'ngInject';
  }
}

export default UserAvatarDirective;
