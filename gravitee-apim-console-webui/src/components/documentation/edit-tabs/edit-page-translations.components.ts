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
import { IController, IScope } from 'angular';

import { DocumentationService, PageType } from '../../../services/documentation.service';
import NotificationService from '../../../services/notification.service';

class EditPageTranslationsComponentController implements IController {
  apiId: string;
  canUpdate: boolean;
  currentTranslation: any;
  pagesToLink: any[];
  page: any;
  readOnly: any;

  constructor(
    private readonly $mdDialog: angular.material.IDialogService,
    private $scope: IScope,
    private readonly DocumentationService: DocumentationService,
    private readonly NotificationService: NotificationService,
  ) {}

  $onInit() {
    this.$scope.$on('saveTranslation', () => {
      this.saveTranslation();
    });
  }

  $onChanges() {
    this.refreshTranslations();
  }

  selectTranslation(translation: any) {
    this.currentTranslation = translation;
    if (!this.currentTranslation.configuration.inheritContent) {
      this.currentTranslation.configuration.inheritContent = 'true';
    }
  }

  addTranslation() {
    this.currentTranslation = {
      type: 'TRANSLATION',
      parentId: this.page.id,
      configuration: {},
    };
    if (
      this.page.type === PageType.MARKDOWN ||
      this.page.type === PageType.SWAGGER ||
      this.page.type === PageType.LINK ||
      this.page.type === PageType.ASCIIDOC ||
      this.page.type === PageType.ASYNCAPI
    ) {
      this.currentTranslation.configuration.inheritContent = 'true';
    }
  }

  refreshTranslations() {
    this.DocumentationService.get(this.apiId, this.page.id).then((response: any) => (this.page.translations = response.data.translations));
    delete this.currentTranslation;
  }

  removeTranslation(page: any) {
    this.$mdDialog
      .show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('html-loader!../../dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
        clickOutsideToClose: true,
        locals: {
          title: 'Would you like to remove "' + page.name + '"?',
          confirmButton: 'Remove',
        },
      })
      .then((response: any) => {
        if (response) {
          this.DocumentationService.remove(page.id, this.apiId).then(() => {
            this.NotificationService.show('Translation ' + page.name + ' has been removed');
            this.refreshTranslations();
          });
        }
      });
  }

  saveTranslation() {
    if (
      this.page.configuration &&
      ('page' === this.page.configuration.resourceType || 'category' === this.page.configuration.resourceType)
    ) {
      this.currentTranslation.content = this.page.content;
    }
    // save translation
    if (!this.currentTranslation.id) {
      this.DocumentationService.create(this.currentTranslation, this.apiId).then((response: any) => {
        const page = response.data;
        this.NotificationService.show("'" + page.name + "' has been created");
        this.refreshTranslations();
      });
    } else {
      this.DocumentationService.update(this.currentTranslation, this.apiId).then(() => {
        this.NotificationService.show("'" + this.currentTranslation.name + "' has been updated");
        this.refreshTranslations();
      });
    }
  }

  updateTranslationContent() {
    if (
      this.currentTranslation.configuration.inheritContent === 'false' &&
      (!this.currentTranslation.content || this.currentTranslation.content === '')
    ) {
      this.currentTranslation.content = this.page.content;
    }
    if (this.currentTranslation.configuration.inheritContent === 'true') {
      delete this.currentTranslation.content;
    }
  }
}
EditPageTranslationsComponentController.$inject = ['$mdDialog', '$scope', 'DocumentationService', 'NotificationService'];

export const EditPageTranslationsComponent: ng.IComponentOptions = {
  bindings: {
    apiId: '<',
    canUpdate: '<',
    currentTranslation: '=',
    pagesToLink: '<',
    page: '=',
    readOnly: '<',
  },
  template: require('html-loader!./edit-page-translations.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: EditPageTranslationsComponentController,
};
