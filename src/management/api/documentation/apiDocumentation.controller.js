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
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var _ = require("lodash");
var DocumentationController = (function () {
    function DocumentationController(resolvedPages, DocumentationService, $scope, $state, $location, dragularService) {
        'ngInject';
        var _this = this;
        this.resolvedPages = resolvedPages;
        this.DocumentationService = DocumentationService;
        this.$scope = $scope;
        this.$state = $state;
        this.$location = $location;
        this.dragularService = dragularService;
        this.pages = resolvedPages.data;
        this.DocumentationService = DocumentationService;
        this.editMode = false;
        $scope.listPagesDisplayed = true;
        if (this.pages.length && !$state.params.pageId) {
            $location.url("/apis/" + $state.params.apiId + "/settings/documentation/" + this.pages[0].id);
        }
        $scope.$on('onGraviteePageDeleted', function () {
            _this.$state.go('apis.admin.documentation');
        });
    }
    DocumentationController.prototype.$onInit = function () {
        var _this = this;
        var that = this;
        this.list().then(function () {
            var d = document.querySelector('.pages');
            that.dragularService([d], {
                scope: _this.$scope,
                containersModel: _.cloneDeep(_this.pages),
                nameSpace: 'documentation'
            });
            that.$scope.$on('dragulardrop', function (e, el, target, source, dragularList, index) {
                var movedPage = that.pages[index];
                for (var idx = 0; idx < dragularList.length; idx++) {
                    if (movedPage.id === dragularList[idx].id) {
                        movedPage.order = idx + 1;
                        break;
                    }
                }
                that.pages = dragularList;
                that.DocumentationService.editPage(that.$state.params.apiId, movedPage.id, movedPage).then(function () {
                    that.$state.go("apis.admin.documentation.page", { apiId: that.$state.params.apiId, pageId: movedPage.id });
                });
            });
        });
    };
    DocumentationController.prototype.list = function () {
        var _this = this;
        return this.DocumentationService.list(this.$state.params.apiId).then(function (response) {
            _this.pages = response.data;
            return { pages: _this.pages };
        }).then(function (response) {
            if (response.pages && response.pages.length > 0) {
                if (_this.$state.params.pageId !== undefined) {
                    _this.$state.go("apis.admin.documentation.page", { pageId: _this.$state.params.pageId });
                }
                else {
                    _this.$state.go("apis.admin.documentation.page", { pageId: response.pages[0].id });
                }
            }
            return response;
        });
    };
    DocumentationController.prototype.showNewPageDialog = function (pageType) {
        this.$state.go('apis.admin.documentation.new', { type: pageType });
    };
    return DocumentationController;
}());
exports.default = DocumentationController;
