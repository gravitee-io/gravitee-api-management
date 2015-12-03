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
  
	constructor(DocumentationService, $state, $mdDialog, $rootScope, $scope, NotificationService) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.preview();

    var that = this;
    DocumentationService.get($state.params.apiId, $state.params.pageId).then(function (response) {
      that.page = response.data;
      that.initialPage = _.clone(response.data);
    });
  }

  update() {
    var that = this;
    this.DocumentationService.editPage(this.$scope.$parent.apiCtrl.api.id, this.page.id, this.page).then(function () {
      that.$state.go(that.$state.current, that.$state.params, {reload: true});
    });
  }

  reset() {
    this.preview();
    this.page = _.clone(this.initialPage);
  }

  delete() {
    var alert = this.$mdDialog.confirm({
      title: 'Warning',
      content: 'Are you sure you want to remove the page ' + this.page.id + '?',
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
}

export default PageController;
