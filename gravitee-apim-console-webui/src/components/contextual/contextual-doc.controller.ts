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
import { marked } from 'marked';

class ContextualDocController {
  public isOpen = true;
  public page: any = {};
  private contextualDocVisibilityKey = 'gv-contextual-doc-visibility';

  constructor(private $transitions, private $http, public $state, private $window, private $rootScope) {
    'ngInject';

    // init contextual page visibility
    if ($window.localStorage.getItem(this.contextualDocVisibilityKey) !== null) {
      this.isOpen = JSON.parse($window.localStorage.getItem(this.contextualDocVisibilityKey));
      this.$rootScope.helpDisplayed = this.isOpen;
    } else {
      this.setDocumentationVisibility();
    }

    // init contextual page
    this.changeDocumentationPage($state.current);

    // watch for transition changes
    $transitions.onFinish({}, (trans) => {
      this.changeDocumentationPage(trans.to());
    });

    // watch for open documentation events
    $rootScope.$on('openContextualDocumentation', () => {
      this.openDocumentation();
      this.changeDocumentationPage($state.current);
    });
  }

  openDocumentation() {
    this.isOpen = !this.isOpen;
    this.setDocumentationVisibility();
    this.$rootScope.$broadcast('onWidgetResize');
  }

  changeDocumentationPage(state) {
    if (this.isOpen && state.data && state.data.docs) {
      this.$http.get(`./docs/${state.data.docs.page}.md`).then((response: any) => {
        this.page.content = marked.parse(response.data);
      });
    }
  }

  private setDocumentationVisibility() {
    this.$window.localStorage.setItem(this.contextualDocVisibilityKey, this.isOpen);
    this.$rootScope.helpDisplayed = this.isOpen;
  }
}

export default ContextualDocController;
