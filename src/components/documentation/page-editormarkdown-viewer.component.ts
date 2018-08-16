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
import * as Viewer from "tui-editor/dist/tui-editor-Viewer";

class ComponentCtrl implements ng.IComponentController {

  public page: any;
  public options: any;

  constructor(private $rootScope) {
      "ngInject";
  }

  $onInit() {}

  $onChanges() {
    const initialValue = this.page && this.page.content ? this.page.content : "";

    new Viewer(Object.assign({
      el: document.querySelector("#viewerSection"),
      viewer: true,
      height: "auto",
      initialValue: initialValue
    }, this.options));
  }
}

const PageEditorMarkdownViewerComponent: ng.IComponentOptions = {
  template: require("./page-editormarkdown-viewer.html"),
  bindings: {
    page: "<",
    options: "<"
  },
  controller: ComponentCtrl
};

export default PageEditorMarkdownViewerComponent;
