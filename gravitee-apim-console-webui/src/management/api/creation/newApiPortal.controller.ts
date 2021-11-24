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

// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-option');

import { ConsoleSettings } from '../../../entities/consoleSettings';

export function getDefinitionVersionTitle(definitionVersion) {
  if (definitionVersion === '2.0.0') {
    return 'Design studio';
  }
  return 'Paths based';
}

export function getDefinitionVersionDescription(definitionVersion) {
  if (definitionVersion === '2.0.0') {
    return 'Use this version to enjoy a more intuitive interface to design';
  }
  return 'Use the proven version you will be able to migrate later';
}

class NewApiController {
  definitionVersion: any;
  options: any[];
  isImport: boolean;

  getDefinitionVersionTitle = getDefinitionVersionTitle;
  private definitionVersions: string[];

  private consoleSettings: ConsoleSettings;

  constructor(private policies, private Constants: any, private ConsoleSettingsService: any) {
    'ngInject';
    this.definitionVersions = ['2.0.0', '1.0.0'];
    this.definitionVersion = '2.0.0';
    this.isImport = false;
  }

  $onInit() {
    this.ConsoleSettingsService.get().then(({ data }) => {
      this.consoleSettings = data;
    });
  }

  cancelImport() {
    this.isImport = false;
  }

  getImportTitle() {
    return `Import ${getDefinitionVersionTitle(this.definitionVersion)}`;
  }

  get allowsPathBasedCreation() {
    return this.consoleSettings?.management?.pathBasedApiCreation?.enabled;
  }
}

export default NewApiController;
