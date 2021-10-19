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
import * as codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import { StateService } from '@uirouter/core';
import * as hljs from 'highlight.js';

import NotificationService from '../../../services/notification.service';

class ComponentCtrl implements ng.IComponentController {
  public page: any;
  public options: any;
  public pagesToLink: any[];

  private tuiEditor: any;

  constructor(
    private $http,
    private readonly Constants,
    private $state: StateService,
    private readonly $mdDialog: angular.material.IDialogService,
    private readonly NotificationService: NotificationService,
  ) {
    'ngInject';
  }

  $onChanges() {
    const initialValue = this.page && this.page.content ? this.page.content : '';
    let mediaURL;
    if (this.$state.params.apiId) {
      mediaURL = this.Constants.env.baseURL + '/apis/' + this.$state.params.apiId + '/media/';
    } else {
      mediaURL = this.Constants.env.baseURL + '/portal/media/';
    }
    if (mediaURL.includes('{:envId}')) {
      mediaURL = mediaURL.replace('{:envId}', this.Constants.org.currentEnv.id);
    }
    const toolbarItems = [
      'heading',
      'bold',
      'italic',
      'strike',
      'divider',
      'hr',
      'quote',
      'divider',
      'ul',
      'ol',
      'task',
      'indent',
      'outdent',
      'divider',
      'table',
      'link',
      'divider',
      'code',
      'codeblock',
    ];

    if (this.Constants.env.settings.portal.uploadMedia.enabled) {
      // toolbarItems
      toolbarItems.splice(15, 0, 'image');
    }

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const Editor = require('@toast-ui/editor');
    if (this.tuiEditor) {
      this.tuiEditor.remove();
    }

    this.tuiEditor = new Editor(
      Object.assign(
        {
          el: document.querySelector('#editSection'),
          initialEditType: 'markdown',
          previewStyle: 'vertical',
          initialValue: initialValue,
          useDefaultHTMLSanitizer: false,
          height: '500px',
          usageStatistics: false,
          exts: ['table', 'scrollSync'],
          toolbarItems: toolbarItems,
          events: {
            change: () => {
              this.page.content = this.tuiEditor.getMarkdown();
            },
          },
          hooks: {
            addImageBlobHook: (blob, callback) => {
              const fd = new FormData();
              fd.append('file', blob);

              if (blob.size > this.Constants.env.settings.portal.uploadMedia.maxSizeInOctet) {
                this.NotificationService.showError(
                  `File uploaded is too big, you're limited at ${this.Constants.env.settings.portal.uploadMedia.maxSizeInOctet} bytes`,
                );
                return false;
              }

              this.$http.post(mediaURL + 'upload', fd, { headers: { 'Content-Type': undefined } }).then((response) => {
                callback(mediaURL + response.data, blob.name);
              });

              return false;
            },
          },
          plugins: [[codeSyntaxHighlight, { hljs }]],
        },
        this.options,
      ),
    );

    this.tuiEditor.eventManager.addEventType('addLinkToPage');
    const toolbar = this.tuiEditor.getUI().getToolbar();
    toolbar.insertItem(
      this.Constants.env.settings.portal.uploadMedia.enabled ? 17 : 16, // index depends on image button
      {
        type: 'button',
        options: {
          event: 'addLinkToPage',
          tooltip: 'Insert page link',
          style: "background-image: url('assets/logo_file.svg');  background-size: 20px 20px;",
        },
      },
    );

    this.tuiEditor.eventManager.listen('addLinkToPage', () => {
      this.$mdDialog
        .show({
          controller: 'SelectPageDialogController',
          controllerAs: 'ctrl',
          template: require('../dialog/selectpage.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            pages: this.pagesToLink,
            title: 'Create a link to a page',
          },
        })
        .then((page) => {
          if (page) {
            if (this.$state.params.apiId) {
              const pageLinkTag = `[${page.name}](/#!/apis/${this.$state.params.apiId}/documentation/${page.id})`;
              this.tuiEditor.insertText(pageLinkTag);
            } else {
              const pageLinkTag = `[${page.name}](/#!/settings/pages/${page.id})`;
              this.tuiEditor.insertText(pageLinkTag);
            }
          }
        });
    });
  }
}

export const PageEditorMarkdownComponent: ng.IComponentOptions = {
  template: require('./page-editormarkdown.html'),
  bindings: {
    page: '<',
    options: '<',
    pagesToLink: '<',
  },
  controller: ComponentCtrl,
};
