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
  
	constructor(DocumentationService, $state, $mdDialog, $rootScope, $scope) {
    'ngInject';
    this.DocumentationService = DocumentationService;
    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.$mdDialog = $mdDialog;
    this.editMode = false;

    var that = this;
    DocumentationService.get($state.params.apiId, $state.params.pageId).then(function (response) {
      that.page = response.data;
      that.initialPage = _.clone(response.data);
    });
  }

  initState() {
    this.$scope.pagePublished = this.page.state === 'published'? true : false;
  }

  update() {
    var editPage = {
      'title' : this.page.title,
      'name': this.page.name,
      'content': this.page.content
    };
    var that = this;
    this.DocumentationService.editPage(this.$scope.$parent.apiCtrl.api.id, this.page.id, editPage).then(function () {
      that.$state.go(that.$state.current, that.$state.params, {reload: true});
    });
  }

  reset() {
    this.editMode = false;
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
          that.$rootScope.$broadcast('onGraviteePageDeleted');
        });
      });
  }

  edit() {
    this.editMode = true;
  }

  preview() {
    this.editMode = false;
  }
}

export default PageController;
