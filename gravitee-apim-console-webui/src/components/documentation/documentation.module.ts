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

import * as angular from 'angular';

import { DocumentationManagementComponent } from './documentation-management.component';
import { EditPageComponent } from './edit-page.component';
import { EditLinkContentComponent } from './edit-tabs/edit-link-content.components';
import { EditPageAclsComponent } from './edit-tabs/edit-page-acls.components';
import { EditPageAttachedResourcesComponent } from './edit-tabs/edit-page-attached-resources.components';
import { EditPageConfigurationComponent } from './edit-tabs/edit-page-configuration.components';
import { EditPageContentComponent } from './edit-tabs/edit-page-content.components';
import { EditPageFetchersComponent } from './edit-tabs/edit-page-fetchers.component';
import { EditPageTranslationsComponent } from './edit-tabs/edit-page-translations.components';
import { DocumentationNewFolderComponent } from './folder/new-folder.component';
import { ImportPagesComponent } from './import-pages.component';
import { DocumentationNewLinkComponent } from './link/new-link.component';
import { NewPageComponent } from './new-page.component';
import { PageAsciiDocComponent } from './page/page-asciidoc.component';
import { PageAsyncApiComponent } from './page/page-asyncapi.component';
import { PageEditorMarkdownComponent } from './page/page-editormarkdown.component';
import { PageMarkdownComponent } from './page/page-markdown.component';
import { PageSwaggerComponent } from './page/page-swagger.component';

angular.module('gravitee-component-documentation', []);

const graviteeDocumentationModule = angular.module('gravitee-component-documentation');

graviteeDocumentationModule.component('documentationManagement', DocumentationManagementComponent);
graviteeDocumentationModule.component('gvNewFolder', DocumentationNewFolderComponent);
graviteeDocumentationModule.component('gvNewLink', DocumentationNewLinkComponent);
graviteeDocumentationModule.component('gvEditLinkContent', EditLinkContentComponent);
graviteeDocumentationModule.component('editPage', EditPageComponent);
graviteeDocumentationModule.component('gvEditPageAcls', EditPageAclsComponent);
graviteeDocumentationModule.component('gvEditPageAttachedResources', EditPageAttachedResourcesComponent);
graviteeDocumentationModule.component('gvEditPageContent', EditPageContentComponent);
graviteeDocumentationModule.component('gvEditPageConfiguration', EditPageConfigurationComponent);
graviteeDocumentationModule.component('gvEditPageFetchers', EditPageFetchersComponent);
graviteeDocumentationModule.component('gvEditPageTranslations', EditPageTranslationsComponent);
graviteeDocumentationModule.component('importPages', ImportPagesComponent);
graviteeDocumentationModule.component('newPage', NewPageComponent);
graviteeDocumentationModule.component('gvPageAsciiDoc', PageAsciiDocComponent);
graviteeDocumentationModule.component('gvPageAsyncApi', PageAsyncApiComponent);
graviteeDocumentationModule.component('gvPageEditorMarkdown', PageEditorMarkdownComponent);
graviteeDocumentationModule.component('gvPageMarkdown', PageMarkdownComponent);
graviteeDocumentationModule.component('gvPageSwagger', PageSwaggerComponent);
