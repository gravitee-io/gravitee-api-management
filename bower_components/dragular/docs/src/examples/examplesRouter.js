'use strict';

var examplesAppModule = require('./examplesApp');

/**
 * @ngInject
 */

examplesAppModule
  .config(function($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise('/home');

    var timer,
      ctrl = function routerCtrl($state, $stateParams, $timeout) {
        // go to install notes by default
        if (!$stateParams.link) {
          timer = $timeout(function timer() {
            $state.go('docs.detail', {
              link: 'docsInstall'
            });
          },0);
        }else{
          $timeout.cancel(timer);
        }
      };

    $stateProvider
      .state('home', {
        url: '/home',
        templateUrl: 'partials/partial-home.html'
      })
      .state('docs', {
        url: '/docs',
        templateUrl: 'partials/partial-docs.html',
        controller: ctrl
      })
      .state('docs.detail', {
        url: '/:link',
        templateUrl: function($stateParams) {
          return $stateParams.link + '/' + $stateParams.link + '.html';
        },
        controller: ctrl
      })
      .state('contribute', {
        url: '/contribute',
        templateUrl: 'partials/partial-contribute.html'
      });
  });
