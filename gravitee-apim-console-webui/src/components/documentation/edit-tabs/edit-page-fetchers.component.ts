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
import angular, { IController, IScope } from 'angular';

interface IPageScope extends IScope {
  fetcherJsonSchema: {
    type: string;
    id: string;
    properties: any;
  };
}

class EditPageFetchersComponentController implements IController {
  fetchers: any[];
  page: any;
  title: string;
  withPublishOption: boolean;

  fetcherJsonSchemaForm: string[];

  constructor(private $scope: IPageScope) {
    'ngInject';
  }

  $onInit() {
    const fetcher = this.fetchers.find((f) => f.id === this.page?.source?.type);
    this.$scope.fetcherJsonSchema = angular.fromJson(fetcher?.schema) || emptyFetcher;
    this.fetcherJsonSchemaForm = ['*'];
  }

  configureFetcher(fetcher: any) {
    if (!this.page.source) {
      this.page.source = {};
    }

    this.page.source = {
      type: fetcher.id,
      configuration: this.page?.source?.configuration || {},
    };
    this.$scope.fetcherJsonSchema = angular.fromJson(fetcher.schema);
  }
}

export const EditPageFetchersComponent: ng.IComponentOptions = {
  bindings: {
    fetchers: '=',
    page: '=',
    title: '<',
    withPublishOption: '<',
  },
  template: require('./edit-page-fetchers.html'),
  controller: EditPageFetchersComponentController,
};

export const emptyFetcher = {
  type: 'object',
  id: 'empty',
  properties: { '': {} },
};
