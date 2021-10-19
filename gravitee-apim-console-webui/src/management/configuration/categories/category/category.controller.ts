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

import { ApiService } from '../../../../services/api.service';
import CategoryService from '../../../../services/category.service';
import NotificationService from '../../../../services/notification.service';

class CategoryController {
  public searchText = '';
  public categoryForm: any;
  private createMode = false;
  private allApis: any[];
  private initialCategory: any;
  private category: any;
  private categoryApis: any[];
  private pages: any[];
  private selectedAPIs: any[];
  private addedAPIs: any[];
  private formChanged = false;

  constructor(
    private ApiService: ApiService,
    private CategoryService: CategoryService,
    private NotificationService: NotificationService,
    private $q: ng.IQService,
    private $filter: ng.IFilterService,
    private $state: StateService,
    private $location: ng.ILocationService,
    private $mdDialog: angular.material.IDialogService,
    private $scope: IScope,
  ) {
    'ngInject';
    this.createMode = $location.path().endsWith('new');
  }

  $onInit() {
    this.addedAPIs = [];
    this.selectedAPIs = this.categoryApis ? this.categoryApis.slice(0) : [];
    this.$scope.$on('categoryPictureChangeSuccess', (event, args) => {
      this.category.picture = args.image;
      this.formChanged = true;
    });
    this.$scope.$on('categoryBackgroundChangeSuccess', (event, args) => {
      this.category.background = args.image;
      this.formChanged = true;
    });
    this.pages = this.pages.sort((a, b) => {
      let comparison = 0;
      const aFullPath = a.parentPath + '/' + a.name;
      const bFullPath = b.parentPath + '/' + b.name;
      if (aFullPath > bFullPath) {
        comparison = 1;
      } else if (aFullPath < bFullPath) {
        comparison = -1;
      }
      return comparison;
    });
    if (this.pages && this.pages.length > 0) {
      this.pages.unshift({});
    }
    this.$scope.$on('apiPictureChangeSuccess', (event, args) => {
      if (!this.category) {
        this.category = {};
      }
      this.category.picture = args.image;
      this.formChanged = true;
    });
    this.initialCategory = _.cloneDeep(this.category);
  }

  reset() {
    this.category = _.cloneDeep(this.initialCategory);
    this.formChanged = false;
    if (this.categoryForm) {
      this.categoryForm.$setPristine();
      this.categoryForm.$setUntouched();
    }
  }

  save() {
    const categoryFunction = this.createMode ? this.CategoryService.create(this.category) : this.CategoryService.update(this.category);
    categoryFunction.then((response) => {
      const category = response.data;
      // update category's apis
      const apiFunctions = this.addedAPIs.map((api) => {
        const apiCategories = api.categories || [];
        apiCategories.push(category.id);
        api.categories = apiCategories;
        return this.ApiService.update(api);
      });
      this.$q.all(apiFunctions).then(() => {
        this.NotificationService.show('Category ' + category.name + ' has been saved.');
        this.$state.go('management.settings.category', { categoryId: category.key }, { reload: true });
      });
    });
  }

  searchAPI(searchText) {
    if (this.allApis) {
      const apisFound = _.filter(this.allApis, (api) => !this.selectedAPIs.some((a) => a.id === api.id));
      return this.$filter('filter')(apisFound, searchText);
    } else {
      return this.ApiService.list().then((response) => {
        // Map the response object to the data object.
        const apis = response.data;
        this.allApis = apis;
        const apisFound = _.filter(apis, (api) => !this.selectedAPIs.some((a) => a.id === api.id));
        return this.$filter('filter')(apisFound, searchText);
      });
    }
  }

  selectedApiChange(api) {
    if (api) {
      this.ApiService.get(api.id).then((response) => {
        this.addedAPIs.push(response.data);
        this.selectedAPIs.push(response.data);
      });
    }
    this.searchText = '';
    this.formChanged = true;
    setTimeout(() => {
      document.getElementById('new-category-apis-autocomplete-id').blur();
    }, 0);
  }

  removeApi(api) {
    this.$mdDialog
      .show({
        controller: 'DeleteAPICategoryDialogController',
        template: require('./delete-api-category.dialog.html'),
        locals: {
          api: api,
        },
      })
      .then((deleteApi) => {
        if (deleteApi) {
          if (this.categoryApis && this.categoryApis.some((a) => a.id === api.id)) {
            // we need to retrieve the API to get the all information required for the update
            this.ApiService.get(api.id).then((response) => {
              const apiFound = response.data;
              _.remove(apiFound.categories, (c) => c === this.category.key);
              this.ApiService.update(apiFound).then(() => {
                this.NotificationService.show("API '" + api.name + "' detached with success");
                _.remove(this.selectedAPIs, api);
                _.remove(this.categoryApis, api);
              });
            });
          } else {
            _.remove(this.selectedAPIs, api);
          }
        }
      });
  }

  toggleHighlightAPI(api) {
    this.category.highlightApi = api.id;
    this.categoryForm.$setDirty();
  }

  toggleVisibility() {
    this.category.hidden = !this.category.hidden;
    this.formChanged = true;
  }

  isHighlightApi(api) {
    return this.category && this.category.highlightApi === api.id;
  }

  getApis() {
    return this.selectedAPIs;
  }
}

export default CategoryController;
