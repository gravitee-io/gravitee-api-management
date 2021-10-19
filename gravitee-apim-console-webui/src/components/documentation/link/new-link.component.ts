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
import { IController } from 'angular';

import { DocumentationService } from '../../../services/documentation.service';

class DocumentationNewLinkComponentController implements IController {
  page: any;
  categoryResources: any;
  foldersById: any;
  systemFoldersById: any;
  pageList: any;
  onSave: () => void;

  constructor(private readonly DocumentationService: DocumentationService) {
    'ngInject';
  }

  checkIfFolder() {
    if (this.page.content) {
      if (this.page.content === 'root') {
        this.page.configuration.isFolder = true;
        this.page.configuration.inherit = 'false';
      } else {
        const folder = this.DocumentationService.getFolder(this.systemFoldersById, this.foldersById, this.page.content);
        if (folder) {
          this.page.configuration.isFolder = true;
        } else {
          this.page.configuration.isFolder = false;
        }
      }
    }
  }

  onChangeLinkType() {
    delete this.page.content;
    delete this.page.configuration.isFolder;
    if (this.page.configuration.resourceType !== 'external' && !this.page.configuration.inherit) {
      this.page.configuration.inherit = 'true';
    }
  }

  save() {
    this.onSave();
  }

  updateLinkName(resourceName: string) {
    if (!this.page.name || this.page.name === '' || this.page.configuration.inherit === 'true' || resourceName === '') {
      this.page.name = resourceName;
    }
  }
}

export const DocumentationNewLinkComponent: ng.IComponentOptions = {
  bindings: {
    categoryResources: '<',
    foldersById: '<',
    page: '=',
    pageList: '<',
    onSave: '&',
    systemFoldersById: '<',
  },
  template: require('./new-link.html'),
  controller: DocumentationNewLinkComponentController,
};
