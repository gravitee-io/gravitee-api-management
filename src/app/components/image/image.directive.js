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
class ImageDirective {
  constructor() {
    let directive = {
      restrict: 'E',
      scope: {
        image: '=',
        imageClass: '@',
        imageDefault: '@',
        imageOriginal: '=?',
        imageForm: '=',
        imageUrl: '=',
        imageBorderRadius: '@'
      },
      templateUrl: 'app/components/image/image.html',
      controller: ImageController,
      controllerAs: 'imgCtrl'
    };

    return directive;
  }
}

class ImageController {
  constructor($rootScope, $scope, Upload) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.Upload = Upload;
  }

  selectImage(file) {
    var that = this;
    this.Upload.base64DataUrl(file).then(function (image) {
      if (image) {
        if (!that.$scope.imageOriginal) {
          that.$scope.imageOriginal = that.$scope.image;
        }
        that.$scope.image = image;
        if (that.$scope.imageForm) {
          that.$scope.imageForm.$setDirty();
        }
        that.$rootScope.$broadcast("apiPictureChangeSuccess", {image: image});
      }
    });
  }

  getSource() {
    if (this.$scope.image && this.$scope.imageOriginal !== this.$scope.image) {
      return this.$scope.image;
    }
    return this.$scope.imageUrl || this.$scope.imageDefault;
  }
}

export default ImageDirective;
