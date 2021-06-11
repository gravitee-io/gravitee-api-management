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
import { PageType } from '../../../services/documentation.service';

interface IPageScope extends IScope {
  editorReadonly: boolean;
}

class EditPageContentComponentController implements IController {
  canUpdate: boolean;
  newPage: boolean;
  page: any;
  pagesToLink: any[];
  pageType: string;

  swaggerCodeMirrorOptions: any;

  constructor(private $scope: IPageScope) {
    'ngInject';
  }

  isSwagger(): boolean {
    return PageType.SWAGGER === this.pageType;
  }
  isMarkdown(): boolean {
    return PageType.MARKDOWN === this.pageType;
  }
  isMarkdownTemplate(): boolean {
    return PageType.MARKDOWN_TEMPLATE === this.pageType;
  }

  $onInit() {
    if (!this.pageType) {
      this.pageType = this.page.type;
    }

    this.swaggerCodeMirrorOptions = {
      viewportMargin: 50,
      lineWrapping: true,
      lineNumbers: true,
      readOnly: this.$scope.editorReadonly,
      mode: { name: 'javascript', json: true },
    };
  }
}

export const EditPageContentComponent: ng.IComponentOptions = {
  bindings: {
    canUpdate: '<',
    newPage: '<',
    page: '<',
    pagesToLink: '<',
    pageType: '<',
  },
  template: require('./edit-page-content.html'),
  controller: EditPageContentComponentController,
};
