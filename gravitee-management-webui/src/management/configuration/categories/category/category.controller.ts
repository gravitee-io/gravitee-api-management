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
import CategoryService from '../../../../services/category.service';
import NotificationService from '../../../../services/notification.service';
import ApiService from '../../../../services/api.service';
import * as _ from 'lodash';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';

class CategoryController {
  public searchText: string = '';
  public categoryForm: any;
  private createMode: boolean = false;
  private allApis: any[];
  private initialCategory: any;
  private category: any;
  private categoryApis: any[];
  private pages: any[];
  private selectedAPIs: any[];
  private addedAPIs: any[];
  private formChanged: boolean = false;

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
    let self = this;
    this.$scope.$on('apiPictureChangeSuccess', function (event, args) {
      if (!self.category) {
        self.category = {};
      }
      self.category.picture = args.image;
      self.formChanged = true;
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
    let that = this;
    let categoryFunction = this.createMode ? this.CategoryService.create(this.category) : this.CategoryService.update(this.category);
    categoryFunction.then((response) => {
      let category = response.data;
      // update category's apis
      let apiFunctions = this.addedAPIs.map((api) => {
        let apiCategories = api.categories || [];
        apiCategories.push(category.id);
        api.categories = apiCategories;
        return that.ApiService.update(api);
      });
      that.$q.all(apiFunctions).then(() => {
        that.NotificationService.show('Category ' + category.name + ' has been saved.');
        that.$state.go('management.settings.category', { categoryId: category.key }, { reload: true });
      });
    });
  }

  searchAPI(searchText) {
    let that = this;
    if (that.allApis) {
      let apisFound = _.filter(that.allApis, (api) => !that.selectedAPIs.some((a) => a.id === api.id));
      return that.$filter('filter')(apisFound, searchText);
    } else {
      return this.ApiService.list().then(function (response) {
        // Map the response object to the data object.
        let apis = response.data;
        that.allApis = apis;
        let apisFound = _.filter(apis, (api) => !that.selectedAPIs.some((a) => a.id === api.id));
        return that.$filter('filter')(apisFound, searchText);
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
    setTimeout(function () {
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
              let apiFound = response.data;
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
