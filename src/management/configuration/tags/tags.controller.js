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
var TagsController = (function () {
    function TagsController(TagService, NotificationService, $q, $mdEditDialog, $mdDialog) {
        'ngInject';
        this.TagService = TagService;
        this.NotificationService = NotificationService;
        this.$q = $q;
        this.$mdEditDialog = $mdEditDialog;
        this.$mdDialog = $mdDialog;
        this.tagsToCreate = [];
        this.tagsToUpdate = [];
    }
    TagsController.prototype.newTag = function (event) {
        var _this = this;
        event.stopPropagation();
        var that = this;
        var promise = this.$mdEditDialog.small({
            placeholder: 'Add a name',
            save: function (input) {
                var tag = { name: input.$modelValue };
                _this.tags.push(tag);
                _this.tagsToCreate.push(tag);
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 30
            }
        });
        promise.then(function (ctrl) {
            var input = ctrl.getInput();
            input.$viewChangeListeners.push(function () {
                input.$setValidity('empty', input.$modelValue.length !== 0);
                input.$setValidity('duplicate', !_.includes(_.map(that.tags, 'name'), input.$modelValue));
            });
        });
    };
    TagsController.prototype.editName = function (event, tag) {
        event.stopPropagation();
        var that = this;
        var promise = this.$mdEditDialog.small({
            modelValue: tag.name,
            placeholder: 'Add a name',
            save: function (input) {
                tag.name = input.$modelValue;
                if (!_.includes(that.tagsToCreate, tag)) {
                    that.tagsToUpdate.push(tag);
                }
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 30
            }
        });
        promise.then(function (ctrl) {
            var input = ctrl.getInput();
            input.$viewChangeListeners.push(function () {
                input.$setValidity('empty', input.$modelValue.length !== 0);
            });
        });
    };
    TagsController.prototype.editDescription = function (event, tag) {
        event.stopPropagation();
        var that = this;
        this.$mdEditDialog.small({
            modelValue: tag.description,
            placeholder: 'Add a description',
            save: function (input) {
                tag.description = input.$modelValue;
                if (!_.includes(that.tagsToCreate, tag)) {
                    that.tagsToUpdate.push(tag);
                }
            },
            targetEvent: event,
            validators: {
                'md-maxlength': 160
            }
        });
    };
    TagsController.prototype.saveTags = function () {
        var that = this;
        this.$q.all([
            this.TagService.create(that.tagsToCreate),
            this.TagService.update(that.tagsToUpdate)
        ]).then(function () {
            that.NotificationService.show("Tags saved with success");
            that.tagsToCreate = [];
            that.tagsToUpdate = [];
        });
    };
    TagsController.prototype.deleteTag = function (tag) {
        var that = this;
        this.$mdDialog.show({
            controller: 'DeleteTagDialogController',
            template: require('./delete.tag.dialog.html'),
            locals: {
                tag: tag
            }
        }).then(function (deleteTag) {
            if (deleteTag) {
                if (tag.id) {
                    that.TagService.delete(tag).then(function () {
                        that.NotificationService.show("Tag '" + tag.name + "' deleted with success");
                        _.remove(that.tags, tag);
                    });
                }
                else {
                    _.remove(that.tagsToCreate, tag);
                    _.remove(that.tags, tag);
                }
            }
        });
    };
    TagsController.prototype.reset = function () {
        this.tags = _.cloneDeep(this.initialTags);
        this.tagsToCreate = [];
        this.tagsToUpdate = [];
    };
    return TagsController;
}());
exports.default = TagsController;
