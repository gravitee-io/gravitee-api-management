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
import { EditorOptions, Editor } from '@toast-ui/editor';
// eslint-disable-next-line import/no-unresolved
import { ToolbarItemOptions } from '@toast-ui/editor/types/ui';
import codeSyntaxHighlightPlugin from '@toast-ui/editor-plugin-code-syntax-highlight';
import Prism from 'prismjs';
import { ActivatedRoute } from '@angular/router';

import NotificationService from '../../../services/notification.service';

// Step 2. Import language files of prismjs that you need
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-groovy';
import 'prismjs/components/prism-xml-doc';

class ComponentCtrl implements ng.IComponentController {
  public page: any;
  public pagesToLink: any[];

  private tuiEditor: Editor;

  constructor(
    private readonly $http: ng.IHttpService,
    private readonly Constants,
    private readonly $mdDialog: angular.material.IDialogService,
    private readonly NotificationService: NotificationService,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  $onChanges() {
    let mediaURL;
    if (this.activatedRoute?.snapshot?.params?.apiId) {
      mediaURL = this.Constants.env.baseURL + '/apis/' + this.activatedRoute.snapshot.params.apiId + '/media/';
    } else {
      mediaURL = this.Constants.env.baseURL + '/portal/media/';
    }
    if (mediaURL.includes('{:envId}')) {
      mediaURL = mediaURL.replace('{:envId}', this.Constants.org.currentEnv.id);
    }
    const toolbarItems: (string | ToolbarItemOptions)[][] = [
      ['heading', 'bold', 'italic', 'strike'],
      ['hr', 'quote'],
      ['ul', 'ol', 'task', 'indent', 'outdent'],
      ['table', 'link'],
      [
        {
          name: '',
          tooltip: 'Insert page link',
          command: 'addLinkToPage',
          style: { backgroundImage: "url('assets/logo_file.svg')", backgroundSize: '30px 30px' },
        },
      ],
      ['code', 'codeblock'],
    ];

    if (this.Constants.env.settings.portal.uploadMedia.enabled) {
      // toolbarItems
      toolbarItems.splice(15, 0, ['image']);
    }

    if (this.tuiEditor) {
      this.tuiEditor.destroy();
    }

    const defaultOptions: EditorOptions = {
      el: document.querySelector('#editSection'),
      initialEditType: 'markdown',
      previewStyle: 'vertical',
      initialValue: this.page && this.page.content ? this.page.content : '',
      height: '500px',
      usageStatistics: false,
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
              `The uploaded file is too big, you are limited to ${this.Constants.env.settings.portal.uploadMedia.maxSizeInOctet} bytes`,
            );
            return false;
          }

          this.$http.post(mediaURL + 'upload', fd, { headers: { 'Content-Type': undefined } }).then((response) => {
            callback(mediaURL + response.data, (blob as File).name);
          });

          return false;
        },
      },
      plugins: [[codeSyntaxHighlightPlugin, { highlighter: Prism }]],
    };
    this.tuiEditor = new Editor(defaultOptions);
    this.tuiEditor.addCommand('markdown', 'addLinkToPage', () => {
      this.$mdDialog
        .show({
          controller: 'SelectPageDialogController',
          controllerAs: 'ctrl',
          template: require('html-loader!../dialog/selectpage.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
          clickOutsideToClose: true,
          locals: {
            pages: this.pagesToLink,
            title: 'Create a link to a page',
          },
        })
        .then((page) => {
          if (page) {
            if (this.activatedRoute?.snapshot?.params?.apiId) {
              const pageLinkTag = `[${page.name}](/#!/apis/${this.activatedRoute?.snapshot?.params?.apiId}/documentation/${page.id})`;
              this.tuiEditor.insertText(pageLinkTag);
            } else {
              const pageLinkTag = `[${page.name}](/#!/settings/pages/${page.id})`;
              this.tuiEditor.insertText(pageLinkTag);
            }
          }
        });
      return true;
    });
  }
}
ComponentCtrl.$inject = ['$http', 'Constants', '$mdDialog', 'NotificationService'];

export const PageEditorMarkdownComponent: ng.IComponentOptions = {
  template: require('html-loader!./page-editormarkdown.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  bindings: {
    page: '<',
    pagesToLink: '<',
  },
  controller: ComponentCtrl,
};
