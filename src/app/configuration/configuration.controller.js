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
class ConfigurationController {
  constructor($scope, ViewService, NotificationService, $q, $mdEditDialog, $mdDialog) {
    'ngInject';

    this.$scope = $scope;
    this.ViewService = ViewService;
    this.NotificationService = NotificationService;
    this.$q = $q;
    this.$mdEditDialog = $mdEditDialog;
    this.$mdDialog = $mdDialog;

    this.loadViews();
    this.viewsToCreate = [];
    this.viewsToUpdate = [];
  }

  loadViews() {
    var that = this;
    this.ViewService.list().then(function (response) {
      that.views = response.data;
      that.initialViews = _.cloneDeep(that.views);
    });
  }

  newView(event) {
    event.stopPropagation();

    var that = this;

    var promise = this.$mdEditDialog.small({
      placeholder: 'Add a name',
      save: function (input) {
        var view = {name: input.$modelValue};
        that.views.push(view);
        that.viewsToCreate.push(view);
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
        input.$setValidity('duplicate', !_.includes(_.map(that.views, 'name'), input.$modelValue));
      });
    });
  }

  editName(event, view) {
    event.stopPropagation();

    var that = this;

    var promise = this.$mdEditDialog.small({
      modelValue: view.name,
      placeholder: 'Add a name',
      save: function (input) {
        view.name = input.$modelValue;
        if (!_.includes(that.viewsToCreate, view)) {
          that.viewsToUpdate.push(view);
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

  editDescription(event, view) {
    event.stopPropagation();

    var that = this;

    this.$mdEditDialog.small({
      modelValue: view.description,
      placeholder: 'Add a description',
      save: function (input) {
        view.description = input.$modelValue;
        if (!_.includes(that.viewsToCreate, view)) {
          that.viewsToUpdate.push(view);
        }
      },
      targetEvent: event,
      validators: {
        'md-maxlength': 60
      }
    });
  }

  saveViews() {
    var that = this;

    this.$q.all([
      this.ViewService.create(that.viewsToCreate),
      this.ViewService.update(that.viewsToUpdate)
    ]).then(function () {
      that.NotificationService.show("Views saved with success");
      that.loadViews();
      that.viewsToCreate = [];
      that.viewsToUpdate = [];
    });
  }

  deleteView(view) {
    var that = this;
    this.$mdDialog.show({
      controller: 'DeleteDialogController',
      templateUrl: 'app/configuration/delete.dialog.html',
      view: view
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

  reset() {
    this.views = _.cloneDeep(this.initialViews);
    this.viewsToCreate = [];
    this.viewsToUpdate = [];
  }
}

export default ConfigurationController;
