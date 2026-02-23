/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness } from '@angular/cdk/testing';

import { DocumentationFolderComponentHarness } from './documentation-folder/documentation-folder.component.harness';
import { NavigationItemContentViewerHarness } from '../../../components/navigation-item-content-viewer/navigation-item-content-viewer.harness';

export class DocumentationComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-documentation';

  private readonly getDocumentationFolder = this.locatorForOptional(DocumentationFolderComponentHarness);
  private readonly getDocumentationPage = this.locatorForOptional(NavigationItemContentViewerHarness);

  async getFolder(): Promise<DocumentationFolderComponentHarness | null> {
    return this.getDocumentationFolder();
  }

  async getPage(): Promise<NavigationItemContentViewerHarness | null> {
    return this.getDocumentationPage();
  }
}
