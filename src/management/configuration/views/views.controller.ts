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
import { StateService } from '@uirouter/core';
import PortalConfigService from "../../../services/portalConfig.service";
import {IScope} from 'angular';

class ViewsController {
  private viewsToUpdate: any[];
  private views: any[];
  private Constants: any;
  private settings: any;

  constructor(
    private ViewService: ViewService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $mdDialog: angular.material.IDialogService,
    private $state: StateService,
    private PortalConfigService: PortalConfigService,
    Constants: any,
    private $rootScope: IScope) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.settings = _.cloneDeep(Constants);
    this.Constants = Constants;
    this.viewsToUpdate = [];
  }

  $onInit() {
    this.views = _.sortBy(this.views, 'order');
    _.forEach(this.views, (view, idx) => {
      view.order = idx;
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
    let that = this;
    view.hidden = !view.hidden;
    this.ViewService.update(view).then(() => {
      that.NotificationService.show('View ' + view.name + ' has been saved.');
    })
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

  toggleDisplayMode() {
    this.PortalConfigService.save(this.settings).then( (response) => {
      _.merge(this.Constants, response.data);
      this.NotificationService.show("Display mode saved!");
    });
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
    this.ViewService.updateViews(that.viewsToUpdate).then(() => {
      that.NotificationService.show("Views saved with success");
      that.viewsToUpdate = [];
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
        that.ViewService.delete(view).then(function () {
          that.NotificationService.show("View '" + view.name + "' deleted with success");
          _.remove(that.views, view);
        });
      }
    });
  }
}

export default ViewsController;
