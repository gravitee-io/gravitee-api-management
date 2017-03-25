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
import * as _ from 'lodash';

export class ApiDocsController {

  private pages: any[];
  private selectedPage: any;
  
  constructor () {
    'ngInject';
    this.initPages();
  }

  initPages() {
    this.pages = [{
      "id" : "1",
      "name" : "Page 1",
      "type" : "MARKDOWN",
      "content" : "It's very easy to make some words **bold** and other words *italic* with Markdown. You can even [link to Google!](http://google.com)"
    },
    {
      "id" : "2",
      "name" : "Page 2",
      "type" : "SWAGGER",
      "path" : "###testSWAGGER"
    },
    {
      "id" : "3",
      "name" : "Page 3",
      "type" : "RAML",
      "path" : "assets/docs/example.raml"
    }];
    this.selectedPage = this.pages[0];
  }

  selectPage(id) {
    this.selectedPage = _.find(this.pages, { 'id': id });
  }
}
