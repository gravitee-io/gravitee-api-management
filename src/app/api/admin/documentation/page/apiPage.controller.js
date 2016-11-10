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
class PageController {

	constructor(DocumentationService, $state, $mdDialog, $rootScope, $scope, NotificationService, FetcherService, $mdSidenav) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.$mdDialog = $mdDialog;
    this.$mdSidenav = $mdSidenav;
    this.NotificationService = NotificationService;
    this.FetcherService = FetcherService;
    var that = this;

    this.useFetcher = false;


    this.$scope.$watch('pageContentFile.content', function (data) {
      if (data) {
        that.page.content = data;
      }
    });

    this.emptyFetcher = {
      "type": "object",
      "id": "empty",
      "properties": {"" : {}}
    };

    this.$scope.fetcherJsonSchema = this.emptyFetcher;
    this.$scope.fetcherJsonSchemaForm = ["*"];
    FetcherService.list().then(response => {
      that.fetchers = response.data;
      if ( $state.current.name === 'apis.admin.documentation.new' ) {
        if (['SWAGGER', 'RAML', 'MARKDOWN'].indexOf($state.params.type) === -1) {
          $state.go('apis.admin.documentation');
        }
        this.createMode = true;
        this.page = { type: this.$state.params.type };
        that.initialPage = _.clone(this.page);
        this.edit();
      } else {
        this.preview();
        DocumentationService.get($state.params.apiId, $state.params.pageId).then( response => {
          that.page = response.data;
          DocumentationService.cachePageConfiguration($state.params.apiId, that.page);
          that.initialPage = _.clone(response.data);
          if(!(_.isNil(that.page.source) || _.isNil(that.page.source.type))) {
            that.useFetcher = true;
            _.forEach(this.fetchers, fetcher => {
              if (fetcher.id === that.page.source.type) {
                this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
              }
            });
          }
        });
      }
    });
  }

  toggleUseFetcher() {
    this.$scope.fetcherJsonSchema = this.emptyFetcher;
    this.page.source = {};
  }

  configureFetcher(fetcher) {
    if (! this.page.source) {
      this.page.source = {};
    }

    this.page.source = {
      type: fetcher.id,
      configuration: {}
    };
    this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
  }

  upsert() {
    var that = this;
    if ( !this.useFetcher && this.page.source ) {
      delete this.page.source;
    }
    if(this.createMode) {
      this.DocumentationService.createPage(this.$state.params.apiId, this.page)
        .then(function (page) {
          that.onPageUpdate();
          that.$state.go('apis.admin.documentation.page', {apiId: that.$state.params.apiId,pageId: page.data.id}, {reload: true});
        })
        .catch(function (error) {
          that.$scope.error = error;
      });
    } else {
      this.DocumentationService.editPage(this.$state.params.apiId, this.page.id, this.page)
        .then(function () {
          that.onPageUpdate();
          that.$state.go(that.$state.current, that.$state.params, {reload: true});
        })
        .catch(function (error) {
          that.$scope.error = error;
        });
    }
  }

  reset() {
    this.preview();
    if (this.initialPage) {
      this.page = _.clone(this.initialPage);
    }
  }

  delete() {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove the page "' + this.page.name + '"?',
      ok: 'OK',
      cancel: 'Cancel'
    });

    var that = this;

    this.$mdDialog
      .show(alert)
      .then(function () {
        that.DocumentationService.deletePage(that.$scope.$parent.apiCtrl.api.id, that.page.id).then(function () {
          that.preview();
          that.$rootScope.$broadcast('onGraviteePageDeleted');
        });
      });
  }

  edit() {
    this.editMode = true;
    this.$scope.$parent.listPagesDisplayed = false;
    if(this.page.source) {
      this.useFetcher = true;
      _.forEach(this.fetchers, (fetcher) => {
        if(fetcher.id === this.page.source.type) {
          this.$scope.fetcherJsonSchema = JSON.parse(fetcher.schema);
        }
      });
    }
  }

  showSettings() {
    this.$mdSidenav('page-settings').toggle();
  }

  preview() {
    this.editMode = false;
    this.$scope.$parent.listPagesDisplayed = true;
  }

  changePublication() {
    var editPage = _.clone(this.initialPage);
    editPage.published = this.page.published;
    var that = this;
    this.DocumentationService.editPage(this.$scope.$parent.apiCtrl.api.id, this.page.id, editPage).then(function () {
      that.$scope.$parent.documentationCtrl.list();
      that.NotificationService.show('Page ' + editPage.name + ' has been ' + (editPage.published ? '':'un') + 'published with success');
    });
  }

  hasNoTitle() {
    return _.isNil(this.page) || _.isNil(this.page.name) || _.isEmpty(this.page.name);
  }

  onPageUpdate() {
    this.NotificationService.show('Page \'' + this.page.name + '\' saved');
  }
}

export default PageController;
