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
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import * as _ from 'lodash';

import CategoryService from '../../../services/category.service';
import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';

class CategoriesController {
  public providedConfigurationMessage = 'Configuration provided by the system';
  private categoriesToUpdate: any[];
  private categories: any[];
  private Constants: any;
  private settings: any;

  constructor(
    private CategoryService: CategoryService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $mdDialog: angular.material.IDialogService,
    private $state: StateService,
    private PortalSettingsService: PortalSettingsService,
    Constants: any,
    private $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.settings = _.cloneDeep(Constants.env.settings);
    this.Constants = Constants;
    this.categoriesToUpdate = [];
  }

  $onInit() {
    this.categories = _.sortBy(this.categories, 'order');
    _.forEach(this.categories, (category, idx) => {
      category.order = idx;
    });
  }

  toggleVisibility(category) {
    category.hidden = !category.hidden;
    this.CategoryService.update(category).then(() => {
      this.NotificationService.show('Category ' + category.name + ' has been saved.');
    });
  }

  upward(index) {
    if (index > 0) {
      this.reorder(index, index - 1);
    }
  }

  downward(index) {
    if (index < _.size(this.categories) - 1) {
      this.reorder(index, index + 1);
    }
  }

  toggleDisplayMode() {
    this.PortalSettingsService.save(this.settings).then((response) => {
      _.merge(this.Constants.env.settings, response.data);
      this.NotificationService.show('Display mode saved!');
    });
  }

  deleteCategory(category) {
    this.$mdDialog
      .show({
        controller: 'DeleteCategoryDialogController',
        template: require('./delete.category.dialog.html'),
        locals: {
          category: category,
        },
      })
      .then((deleteCategory) => {
        if (deleteCategory) {
          this.CategoryService.delete(category).then(() => {
            this.NotificationService.show("Category '" + category.name + "' deleted with success");
            _.remove(this.categories, category);
          });
        }
      });
  }

  isReadonlySetting(property: string): boolean {
    return this.PortalSettingsService.isReadonly(this.settings, property);
  }

  private reorder(from, to) {
    this.categories[from].order = to;
    this.categories[to].order = from;
    this.categories = _.sortBy(this.categories, 'order');

    this.categoriesToUpdate.push(this.categories[from]);
    this.categoriesToUpdate.push(this.categories[to]);
    this.save();
  }

  private save() {
    this.CategoryService.updateCategories(this.categoriesToUpdate).then(() => {
      this.NotificationService.show('Categories saved with success');
      this.categoriesToUpdate = [];
    });
  }
}

export default CategoriesController;
