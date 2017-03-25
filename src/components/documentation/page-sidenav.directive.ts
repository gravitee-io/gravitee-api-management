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
import * as angular from 'angular';

class PageSidenavController {
  constructor (private $timeout: ng.ITimeoutService, private $document, private $window) {
    'ngInject';
  }
}

const PageSidenavDirective: ng.IDirective = ({
  restrict: 'E',
  template: require('./page-sidenav.html'),
  link: function (scope, elem, attr, ctr: {$timeout: ng.ITimeoutService, $window, $document}) {
    ctr.$timeout(function () {
      let sidenav = angular.element(document.getElementById('sidenav'));
      let page = document.getElementById('page-content');

//      let h1Elements = page.querySelectorAll('h1');
      let h2Elements = page.querySelectorAll('h2');

      scope.anchors = _.map(h2Elements, function(elt) {
        return {id: elt.id, title: elt.textContent};
      });

      scope.scrollTo = function(anchor) {
        let scrollElt = document.getElementById(anchor);
        ctr.$document
          .scrollToElementAnimated(scrollElt)
          .catch(function() {});
      };

      angular.element(ctr.$window).bind("scroll", function() {
        if (this.pageYOffset > 280) {
          sidenav.addClass('sidenav-fixed');
        } else {
          sidenav.removeClass('sidenav-fixed');
        }
      });

    }, 200);
  },
  controller: PageSidenavController
});

export default PageSidenavDirective;
