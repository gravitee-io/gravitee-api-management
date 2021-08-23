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

import { PageType } from '../../../services/documentation.service';

class EditPageConfigurationComponentController implements IController {
  page: any;

  shouldShowOpenApiDocFormat = false;
  settings: any;

  constructor(private readonly Constants: any) {
    'ngInject';
  }

  isSwagger(): boolean {
    return PageType.SWAGGER === this.page.type;
  }
  isMarkdownTemplate(): boolean {
    return PageType.MARKDOWN_TEMPLATE === this.page.type;
  }

  $onInit() {
    this.settings = this.Constants.env.settings;

    this.shouldShowOpenApiDocFormat =
      this.settings &&
      this.settings.openAPIDocViewer &&
      this.settings.openAPIDocViewer.openAPIDocType.swagger.enabled &&
      this.settings.openAPIDocViewer.openAPIDocType.redoc.enabled;

    if (this.page.type === 'SWAGGER' && !this.page.configuration.viewer) {
      if (this.settings && this.settings.openAPIDocViewer) {
        this.page.configuration.viewer = this.settings.openAPIDocViewer.openAPIDocType.defaultType;
      }
    }
  }

  openApiFormatLabel(format: string) {
    if (this.settings && this.settings.openAPIDocViewer && format === this.settings.openAPIDocViewer.openAPIDocType.defaultType) {
      return `${format} (Default)`;
    } else {
      return format;
    }
  }

  toggleEntrypointAsServer() {
    if (this.page.configuration.entrypointsAsServers === undefined) {
      // Enable adding context-path automatically only the first time user decides to use entrypoint url.
      this.page.configuration.entrypointAsBasePath = 'true';
    }
  }

  usedAsGeneralConditions() {
    return this.page.generalConditions;
  }
}

export const EditPageConfigurationComponent: ng.IComponentOptions = {
  bindings: {
    page: '=',
  },
  template: require('./edit-page-configuration.html'),
  controller: EditPageConfigurationComponentController,
};
