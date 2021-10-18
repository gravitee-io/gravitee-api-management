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
import * as hljs from 'highlight.js';
import * as _ from 'lodash';
import * as remark from 'remark';

class ComponentCtrl implements ng.IComponentController {
  private page: any;
  private options: any;

  constructor(private $location) {
    'ngInject';
  }

  $onChanges() {
    const initialValue = this.page && this.page.content ? this.page.content : '';
    let ast = remark.parse(initialValue);

    let content = '';
    let sectionOpen = false;

    let sectionValue = '';
    for (let c = 0; c < ast.children.length; c++) {
      const child = ast.children[c];

      if (child.type !== 'heading' || child.depth < 2 || child.depth > 2) {
        continue;
      }

      if (sectionOpen) {
        sectionValue += '</section>';
      }

      let value = this.findChildrenValue(child.children);
      if (value) {
        let id = value.replace(new RegExp(' ', 'g'), '').toLowerCase();
        sectionValue += "<section id='" + id + "'>";
      } else {
        sectionValue += '<section>';
      }
      sectionOpen = true;

      ast.children.splice(c, 0, {
        type: 'html',
        value: sectionValue,
      });
      c++;
      sectionValue = '';
    }

    if (sectionOpen) {
      ast.children.splice(ast.children.length - 1, 0, {
        type: 'html',
        value: sectionValue,
      });
    }

    content = remark.stringify(ast);

    const Viewer = require('@toast-ui/editor/dist/toastui-editor-viewer');
    // tslint:disable-next-line:no-unused-expression
    new Viewer(
      Object.assign(
        {
          el: document.querySelector('#viewerSection'),
          viewer: true,
          height: 'auto',
          initialValue: _.replace(
            _.replace(content, '(#', '(' + this.$location.absUrl() + '#'),
            'href="#',
            'href="' + this.$location.absUrl() + '#',
          ),
          useDefaultHTMLSanitizer: false,
          plugins: [[codeSyntaxHighlight, { hljs }]],
        },
        this.options,
      ),
    );
  }

  findChildrenValue = (children: any, count: number = 0) => {
    if (count > 5) {
      return;
    }
    count++;

    if (children && Array.isArray(children) && children.length > 0) {
      return children[0].type === 'text' ? children[0].value : this.findChildrenValue(children[0].children, count);
    }
    return;
  };
}

const PageEditorMarkdownViewerComponent: ng.IComponentOptions = {
  template: require('./page-editormarkdown-viewer.html'),
  bindings: {
    page: '<',
    options: '<',
  },
  controller: ComponentCtrl,
};

export default PageEditorMarkdownViewerComponent;
