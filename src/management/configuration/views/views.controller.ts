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
import ViewService from '../../../services/view.service';
import NotificationService from '../../../services/notification.service';

class ViewsController {
  private viewsToCreate: any[];
  private viewsToUpdate: any[];
  private initialViews: any[];
  private views: any[];

  constructor(
    private ViewService: ViewService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $mdEditDialog,
    private $mdDialog: angular.material.IDialogService) {
    'ngInject';

    this.viewsToCreate = [];
    this.viewsToUpdate = [];
  }

  $onInit() {
    this.views = _.sortBy(this.views, 'order');
    _.forEach(this.views, (view, idx) => {
      view.order = idx;
    });
  }

  newView(event) {
    event.stopPropagation();

    this.$mdEditDialog
      .small({
        placeholder: 'Add a name',
        save: input =>{
          const view = {
            name: input.$modelValue,
            order: _.size(this.views),
            hidden: false
          };
          this.viewsToCreate.push(view);
          this.save();
        },
        targetEvent: event,
        validators: {
          'md-maxlength': 30
        }
      })
      .then((ctrl) => {
        const input = ctrl.getInput();

        input.$viewChangeListeners.push(() => {
          input.$setValidity('empty', input.$modelValue.length !== 0);
          input.$setValidity('duplicate', !_.includes(_.map(this.views, 'name'), input.$modelValue));
        });
      });
  }

  toggleDefault(view) {
    _.forEach(this.views, (v) => {
      if (v.id !== view.id && v.defaultView) {
        v.defaultView = false;
        this.viewsToUpdate.push(v);
      }
    });
    this.viewsToUpdate.push(view);
    this.save();
  }

  toggleVisibility(view) {
    view.hidden = !view.hidden;
    this.viewsToUpdate.push(view);
    this.save();
  }

  editName(event, view) {
    event.stopPropagation();

    let that = this;

    let promise = this.$mdEditDialog.small({
      modelValue: view.name,
      placeholder: 'Add a name',
      clickOutsideToClose: false,
      save: function (input) {
        view.name = input.$modelValue;
        if (!_.includes(that.viewsToCreate, view)) {
          that.viewsToUpdate.push(view);
          that.save();
        }
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 30
      }
    });

    promise.then(function (ctrl) {
      let input = ctrl.getInput();

      input.$viewChangeListeners.push(function () {
        input.$setValidity('empty', input.$modelValue.length !== 0);
      });
    });
  }

  editDescription(event, view) {
    event.stopPropagation();

    let that = this;

    this.$mdEditDialog.small({
      modelValue: view.description,
      placeholder: 'Add a description',
      clickOutsideToClose: false,
      save: function (input) {
        view.description = input.$modelValue;
        if (!_.includes(that.viewsToCreate, view)) {
          that.viewsToUpdate.push(view);
          that.save();
        }
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 160
      }
    });
  }

  upward(index) {
    if (index > 0) {
      this.reorder(index, index - 1);
    }
  }

  downward(index) {
    if (index < _.size(this.views) - 1 ) {
      this.reorder(index, index + 1);
    }
  }

  private reorder(from, to) {
    this.views[from].order = to;
    this.views[to].order = from;
    this.views = _.sortBy(this.views, 'order');

    this.viewsToUpdate.push(this.views[from]);
    this.viewsToUpdate.push(this.views[to]);
    this.save();
  }

  private save() {
    let that = this;

    this.$q.all([
      this.ViewService.create(that.viewsToCreate),
      this.ViewService.update(that.viewsToUpdate)
    ]).then(function (resultArray) {
      that.NotificationService.show("Views saved with success");
      that.viewsToCreate = [];
      that.viewsToUpdate = [];
      let createResult = resultArray[0];
      if (createResult) {
        that.views = _.sortBy(_.unionBy(createResult.data, that.views, 'name'), 'order');
      }
    });
  }

  deleteView(view) {
    let that = this;
    this.$mdDialog.show({
      controller: 'DeleteViewDialogController',
      template: require('./delete.view.dialog.html'),
      locals: {
        view: view
      }
    }).then(function (deleteView) {
      if (deleteView) {
        if (view.id) {
          that.ViewService.delete(view).then(function () {
            that.NotificationService.show("View '" + view.name + "' deleted with success");
            _.remove(that.views, view);
          });
        } else {
          _.remove(that.viewsToCreate, view);
          _.remove(that.views, view);
        }
      }
    });
  }
}

export default ViewsController;
