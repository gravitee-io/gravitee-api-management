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

import * as jdenticon from 'jdenticon';

class IdentityPictureDirective {
  constructor() {
    'ngInject';

    const directive = {
      restrict: 'E',
      require: 'gvThemeElement',
      scope: {
        image: '<',
        imageDefault: '=',
        imageUrl: '<',
        imageBorderRadius: '@',
        imageName: '=',
        imageWidth: '=',
        imageId: '=',
        imageTheme: '<',
        noDefaultImage: '=',
      },
      template: require('./identityPicture.html'),
      controller: IdentityPictureController,
      controllerAs: 'identityPictureCtrl',
    };

    return directive;
  }
}

class IdentityPictureController {
  constructor(private $scope) {
    'ngInject';
    $scope.imgError = function () {
      document.querySelector('#avatar_' + $scope.imageId).classList.remove('show');
      const div: HTMLElement = document.querySelector('#jdenticon_' + $scope.imageId);
      div.classList.add('show');
      if ($scope.noDefaultImage) {
        div.title = 'No image defined';
        div.innerHTML = 'No image defined';
      } else {
        div.title = $scope.imageName;
        div.innerHTML = jdenticon.toSvg($scope.imageName, $scope.imageWidth ? $scope.imageWidth : 110, { backColor: '#FFF' });
      }
      $scope.$apply();
    };
    $scope.imgLoad = function () {
      document.querySelector('#avatar_' + $scope.imageId).classList.add('show');
      document.querySelector('#jdenticon_' + $scope.imageId).classList.remove('show');
      $scope.$apply();
    };
  }

  getSource() {
    if (this.$scope.image) {
      return this.$scope.image;
    }
    return this.$scope.imageUrl || this.$scope.imageDefault || '';
  }
}

export default IdentityPictureDirective;
