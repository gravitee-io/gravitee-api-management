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
var TagService = (function () {
    function TagService($http, Constants) {
        'ngInject';
        this.$http = $http;
        this.tagsURL = Constants.baseURL + "configuration/tags/";
    }
    TagService.prototype.list = function () {
        return this.$http.get(this.tagsURL);
    };
    TagService.prototype.create = function (tags) {
        if (tags && tags.length) {
            return this.$http.post(this.tagsURL, tags);
        }
    };
    TagService.prototype.update = function (tags) {
        if (tags && tags.length) {
            return this.$http.put(this.tagsURL, tags);
        }
    };
    TagService.prototype.delete = function (tag) {
        if (tag) {
            return this.$http.delete(this.tagsURL + tag.id);
        }
    };
    return TagService;
}());
exports.default = TagService;
