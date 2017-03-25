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
var DocumentationService = (function () {
    function DocumentationService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.swaggerConfigurationCache = {};
        this.documentationURL = function (apiId) { return Constants.baseURL + "apis/" + apiId + "/pages/"; };
    }
    DocumentationService.prototype.list = function (apiId) {
        return this.$http.get(this.documentationURL(apiId));
    };
    DocumentationService.prototype.get = function (apiId, pageId) {
        if (pageId) {
            return this.$http.get(this.documentationURL(apiId) + pageId);
        }
    };
    DocumentationService.prototype.getContentUrl = function (apiId, pageId) {
        return this.documentationURL(apiId) + pageId + '/content';
    };
    DocumentationService.prototype.createPage = function (apiId, newPage) {
        return this.$http.post(this.documentationURL(apiId), newPage);
    };
    DocumentationService.prototype.deletePage = function (apiId, pageId) {
        return this.$http.delete(this.documentationURL(apiId) + pageId);
    };
    DocumentationService.prototype.editPage = function (apiId, pageId, editPage) {
        return this.$http.put(this.documentationURL(apiId) + pageId, {
            name: editPage.name,
            description: editPage.description,
            order: editPage.order,
            published: editPage.published,
            content: editPage.content || '',
            source: editPage.source,
            configuration: editPage.configuration
        });
    };
    DocumentationService.prototype.cachePageConfiguration = function (apiId, page) {
        if (!_.isNil(page) && page.type === 'SWAGGER' && !_.isNil(page.configuration)) {
            var contentUrl = this.getContentUrl(apiId, page.id);
            var url;
            try {
                url = new URL(contentUrl);
            }
            catch (error) {
                url = new URL(location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '') + this.getContentUrl(apiId, page.id));
            }
            this.swaggerConfigurationCache[url.pathname] = page.configuration;
        }
    };
    DocumentationService.prototype.getPageConfigurationFromCache = function (pageContentUrl) {
        return this.swaggerConfigurationCache[pageContentUrl];
    };
    return DocumentationService;
}());
exports.default = DocumentationService;
