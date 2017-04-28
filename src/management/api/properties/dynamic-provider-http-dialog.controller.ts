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
function DialogDynamicProviderHttpController($mdDialog: angular.material.IDialogService) {
  'ngInject';

  this.cancel = $mdDialog.cancel;

  this.codeMirrorOptions = {
    lineWrapping: false,
    lineNumbers: true,
    mode: 'javascript',
    readOnly: true,
    controller: this
  };

  this.codemirrorLoaded = function(_editor) {
    this.controller.editor = _editor;

    // Editor part
    const _doc = this.controller.editor.getDoc();

    // Options
    _doc.markClean();
  };

  this.specificationExample = JSON.stringify([{
    "key" : 1,
    "value" : "https://north-europe.company.com/"
  }, {
    "key" : 2,
    "value" : "https://north-europe.company.com/"
  }, {
    "key" : 3,
    "value" : "https://south-asia.company.com/"
  }], null, 2);
}

export default DialogDynamicProviderHttpController;
