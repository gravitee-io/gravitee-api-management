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
import * as _ from 'lodash';
import TagService from '../../../services/tag.service';
import NotificationService from '../../../services/notification.service';

class TagsController {
  private tagsToCreate: any[];
  private tagsToUpdate: any[];
  private initialTags: any;
  private tags: any;

  constructor(
    private TagService: TagService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService) {
    'ngInject';

    this.tagsToCreate = [];
    this.tagsToUpdate = [];
  }

  newTag(event) {
    event.stopPropagation();

    var that = this;

    var promise = this.$mdEditDialog.small({
      placeholder: 'Add a name',
      save: input => {
        const tag = {name: input.$modelValue};
        this.tags.push(tag);
        this.tagsToCreate.push(tag);
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
  }

  editName(event, tag) {
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
  }

  editDescription(event, tag) {
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
  }

  saveTags() {
    var that = this;

    this.$q.all([
      this.TagService.create(that.tagsToCreate),
      this.TagService.update(that.tagsToUpdate)
    ]).then(function () {
      that.NotificationService.show("Tags saved with success");
      that.tagsToCreate = [];
      that.tagsToUpdate = [];
    });
  }

  deleteTag(tag) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DeleteTagDialogController',
      templateUrl: 'configuration/admin/tags/delete.tag.dialog.html',
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
        } else {
          _.remove(that.tagsToCreate, tag);
          _.remove(that.tags, tag);
        }
      }
    });
  }

  reset() {
    this.tags = _.cloneDeep(this.initialTags);
    this.tagsToCreate = [];
    this.tagsToUpdate = [];
  }
}

export default TagsController;
