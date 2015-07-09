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
class GraviteeContributorDirective {
  constructor (graviteeContributor) {
    'ngInject';

    let directive = {
      restrict: 'E',
      scope: {},
      template: '&nbsp;',
      link: linkFunc,
      controller: GraviteeContributorController,
      controllerAs: 'vm'
    };

    return directive;

    function linkFunc(scope, el, attr, vm) {
      let watcher;
      let typist = graviteeContributor(el[0], {
        typeSpeed: 40,
        deleteSpeed: 40,
        pauseDelay: 800,
        loop: true,
        postfix: ' '
      });

      el.addClass('gravitee-contributor');

      watcher = scope.$watch('vm.contributors', () => {
        angular.forEach(vm.contributors, (contributor) => {
          typist.type(contributor.login).pause().delete();
        });
      });

      scope.$on('$destroy', () => {
        watcher();
      });
    }

  }
}

class GraviteeContributorController {
  constructor ($log, graviteeContributorService) {
    'ngInject';

    this.$log = $log;
    this.contributors = [];

    this.activate(graviteeContributorService);
  }

  activate(graviteeContributorService) {
    return this.getContributors(graviteeContributorService).then(() => {
      this.$log.info('Activated Contributors View');
    });
  }

  getContributors(graviteeContributorService) {
    return graviteeContributorService.getContributors(10).then((data) => {
      this.contributors = data;

      return this.contributors;
    });
  }
}

export default GraviteeContributorDirective;
