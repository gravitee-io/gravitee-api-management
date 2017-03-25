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
var PageController = (function () {
    function PageController(DocumentationService, $state, $mdDialog, $rootScope, $scope, NotificationService, FetcherService, $mdSidenav) {
        'ngInject';
        var _this = this;
        this.DocumentationService = DocumentationService;
        this.$state = $state;
        this.$mdDialog = $mdDialog;
        this.$rootScope = $rootScope;
        this.$scope = $scope;
        this.NotificationService = NotificationService;
        this.FetcherService = FetcherService;
        this.$mdSidenav = $mdSidenav;
        this.useFetcher = false;
        this.codeMirrorOptions = {
            lineWrapping: true,
            lineNumbers: true,
            allowDropFileTypes: true,
            autoCloseTags: true,
            mode: "javascript"
        };
        this.$scope.$watch('pageContentFile.content', function (data) {
            if (data) {
                _this.page.content = data;
            }
        });
        this.emptyFetcher = {
            "type": "object",
            "id": "empty",
            "properties": { "": {} }
        };
        this.$scope.fetcherJsonSchema = this.emptyFetcher;
        this.$scope.fetcherJsonSchemaForm = ["*"];
        FetcherService.list().then(function (response) {
            _this.fetchers = response.data;
            if ($state.current.name === 'apis.admin.documentation.new') {
                if (['SWAGGER', 'RAML', 'MARKDOWN'].indexOf($state.params.type) === -1) {
                    $state.go('apis.admin.documentation');
                }
                _this.createMode = true;
                _this.page = { type: _this.$state.params.type };
                _this.initialPage = _.clone(_this.page);
                _this.edit();
            }
            else {
                _this.preview();
                DocumentationService.get($state.params.apiId, $state.params.pageId).then(function (response) {
                    _this.page = response.data;
                    DocumentationService.cachePageConfiguration($state.params.apiId, _this.page);
                    _this.initialPage = _.clone(response.data);
                    if (!(_.isNil(_this.page.source) || _.isNil(_this.page.source.type))) {
                        _this.useFetcher = true;
                        _.forEach(_this.fetchers, function (fetcher) {
                            if (fetcher.id === _this.page.source.type) {
                                _this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
                            }
                        });
                    }
                });
            }
        });
    }
    PageController.prototype.toggleUseFetcher = function () {
        this.$scope.fetcherJsonSchema = this.emptyFetcher;
        this.page.source = {};
    };
    PageController.prototype.configureFetcher = function (fetcher) {
        if (!this.page.source) {
            this.page.source = {};
        }
        this.page.source = {
            type: fetcher.id,
            configuration: {}
        };
        this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
    };
    PageController.prototype.upsert = function () {
        var _this = this;
        if (!this.useFetcher && this.page.source) {
            delete this.page.source;
        }
        if (this.createMode) {
            this.DocumentationService.createPage(this.$state.params.apiId, this.page)
                .then(function (page) {
                _this.onPageUpdate();
                _this.$state.go('apis.admin.documentation.page', { apiId: _this.$state.params.apiId, pageId: page.data.id }, { reload: true });
            })
                .catch(function (error) {
                _this.$scope.error = error;
            });
        }
        else {
            this.DocumentationService.editPage(this.$state.params.apiId, this.page.id, this.page)
                .then(function () {
                _this.onPageUpdate();
                _this.$state.go(_this.$state.current, _this.$state.params, { reload: true });
            })
                .catch(function (error) {
                _this.$scope.error = error;
            });
        }
    };
    PageController.prototype.reset = function () {
        this.preview();
        if (this.initialPage) {
            this.page = _.clone(this.initialPage);
        }
    };
    PageController.prototype.delete = function () {
        var that = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
                title: 'Are you sure you want to remove the page "' + this.page.name + '" ?',
                msg: '',
                confirmButton: 'Remove'
            }
        }).then(function (response) {
            if (response) {
                that.DocumentationService.deletePage(that.$scope.$parent.apiCtrl.api.id, that.page.id).then(function () {
                    that.preview();
                    that.$rootScope.$broadcast('onGraviteePageDeleted');
                });
            }
        });
    };
    PageController.prototype.edit = function () {
        var _this = this;
        if (this.page.type === 'MARKDOWN') {
            this.codeMirrorOptions.mode = 'gfm';
        }
        else if (this.page.type === 'SWAGGER') {
            this.codeMirrorOptions.mode = 'javascript';
        }
        else if (this.page.type === 'RAML') {
            this.codeMirrorOptions.mode = 'yaml';
        }
        this.editMode = true;
        this.$scope.$parent.listPagesDisplayed = false;
        if (this.page.source) {
            this.useFetcher = true;
            _.forEach(this.fetchers, function (fetcher) {
                if (fetcher.id === _this.page.source.type) {
                    _this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
                }
            });
        }
    };
    PageController.prototype.showSettings = function () {
        this.$mdSidenav('page-settings').toggle();
    };
    PageController.prototype.preview = function () {
        this.editMode = false;
        this.$scope.$parent.listPagesDisplayed = true;
    };
    PageController.prototype.changePublication = function () {
        var editPage = _.clone(this.initialPage);
        editPage.published = this.page.published;
        var that = this;
        this.DocumentationService.editPage(this.$scope.$parent.apiCtrl.api.id, this.page.id, editPage).then(function () {
            that.$scope.$parent.documentationCtrl.list();
            that.NotificationService.show('Page ' + editPage.name + ' has been ' + (editPage.published ? '' : 'un') + 'published with success');
        });
    };
    PageController.prototype.hasNoTitle = function () {
        return _.isNil(this.page) || _.isNil(this.page.name) || _.isEmpty(this.page.name);
    };
    PageController.prototype.onPageUpdate = function () {
        this.NotificationService.show('Page \'' + this.page.name + '\' saved');
    };
    return PageController;
}());
exports.default = PageController;
