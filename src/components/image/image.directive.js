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
var ImageDirective = (function () {
    function ImageDirective() {
        var directive = {
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
            template: require('./image.html'),
            controller: ImageController,
            controllerAs: 'imgCtrl'
        };
        return directive;
    }
    return ImageDirective;
}());
var ImageController = (function () {
    function ImageController($rootScope, $scope, Upload) {
        'ngInject';
        this.$rootScope = $rootScope;
        this.$scope = $scope;
        this.Upload = Upload;
    }
    ImageController.prototype.selectImage = function (file) {
        var _this = this;
        this.Upload.base64DataUrl(file).then(function (image) {
            if (image) {
                if (!_this.$scope.imageOriginal) {
                    _this.$scope.imageOriginal = _this.$scope.image;
                }
                _this.$scope.image = image;
                if (_this.$scope.imageForm) {
                    _this.$scope.imageForm.$setDirty();
                }
                _this.$rootScope.$broadcast("apiPictureChangeSuccess", { image: image });
            }
        });
    };
    ImageController.prototype.getSource = function () {
        if (this.$scope.image && this.$scope.imageOriginal !== this.$scope.image) {
            return this.$scope.image;
        }
        return this.$scope.imageUrl || this.$scope.imageDefault;
    };
    return ImageController;
}());
exports.default = ImageDirective;
