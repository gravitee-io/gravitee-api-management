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
import { StateService } from '@uirouter/core';
import * as codeSyntaxHighlight from '@toast-ui/editor-plugin-code-syntax-highlight';
import * as hljs from 'highlight.js';
import NotificationService from '../../../services/notification.service';

class ComponentCtrl implements ng.IComponentController {
  public page: any;
  public options: any;
  public pagesToLink: any[];

  private maxSize: number;
  private tuiEditor: any;

  constructor(
    private $http,
    private Constants,
    private $state: StateService,
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
  ) {
    'ngInject';
    var lastElement = Constants.env.settings.portal.uploadMedia.maxSizeInOctet;
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
    var toolbarItems = [
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

    let $http = this.$http;
    let Constants = this.Constants;
    let maxSize = this.maxSize;

    if (Constants.env.settings.portal.uploadMedia.enabled) {
      // toolbarItems
      toolbarItems.splice(15, 0, 'image');
    }

    const Editor = require('@toast-ui/editor');
    if (this.tuiEditor) {
      this.tuiEditor.remove();
    }

    const that = this;
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
            change: (change) => {
              this.page.content = this.tuiEditor.getMarkdown();
            },
          },
          hooks: {
            addImageBlobHook: function (blob, callback) {
              let fd = new FormData();
              fd.append('file', blob);

              if (blob.size > Constants.env.settings.portal.uploadMedia.maxSizeInOctet) {
                that.NotificationService.showError(
                  `File uploaded is too big, you're limited at ${Constants.env.settings.portal.uploadMedia.maxSizeInOctet} bytes`,
                );
                return false;
              }

              $http.post(mediaURL + 'upload', fd, { headers: { 'Content-Type': undefined } }).then((response) => {
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
      Constants.env.settings.portal.uploadMedia.enabled ? 17 : 16, // index depends on image button
      {
        type: 'button',
        options: {
          event: 'addLinkToPage',
          tooltip: 'Insert page link',
          style: "background-image: url('assets/logo_file.svg');  background-size: 20px 20px;",
        },
      },
    );

    this.tuiEditor.eventManager.listen('addLinkToPage', function () {
      that.$mdDialog
        .show({
          controller: 'SelectPageDialogController',
          controllerAs: 'ctrl',
          template: require('../dialog/selectpage.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            pages: that.pagesToLink,
            title: 'Create a link to a page',
          },
        })
        .then((page) => {
          if (page) {
            if (that.$state.params.apiId) {
              const pageLinkTag = `[${page.name}](/#!/apis/${that.$state.params.apiId}/documentation/${page.id})`;
              that.tuiEditor.insertText(pageLinkTag);
            } else {
              const pageLinkTag = `[${page.name}](/#!/settings/pages/${page.id})`;
              that.tuiEditor.insertText(pageLinkTag);
            }
          }
        });
    });
  }
}

const PageEditorMarkdownComponent: ng.IComponentOptions = {
  template: require('./page-editormarkdown.html'),
  bindings: {
    page: '<',
    options: '<',
    pagesToLink: '<',
  },
  controller: ComponentCtrl,
};

export default PageEditorMarkdownComponent;
