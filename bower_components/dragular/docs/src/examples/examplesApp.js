/* global angular, hljs */
'use strict';

// var angular = require('angular');

var dragular = require('../../../src/dragularModule');
var examplesRouter = require('./examplesRouter');
var BasicCtrl = require('./exampleBasic/exampleBasic');
var BasicModelCtrl = require('./exampleBasicWithModel/exampleBasicWithModel');
var BoundingBoxCtrl = require('./exampleBoundingBox/exampleBoundingBox');
var BoundingBoxLockXCtrl = require('./exampleBoundingBoxLockX/exampleBoundingBoxLockX');
var BoundingBoxLockYCtrl = require('./exampleBoundingBoxLockY/exampleBoundingBoxLockY');
var CopyCtrl = require('./exampleCopy/exampleCopy');
var CopyModelCtrl = require('./exampleCopyWithModel/exampleCopyWithModel');
var CustomClassesCtrl = require('./exampleCustomClasses/exampleCustomClasses');
var DifferentOptionsModelCtrl = require('./exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel');
var DirectiveCtrl = require('./exampleDirective/exampleDirective');
var DirectiveModelCtrl = require('./exampleDirectiveWithModel/exampleDirectiveWithModel');
var DragOverEventsCtrl = require('./exampleDragOverEvents/exampleDragOverEvents');
var EventsCtrl = require('./exampleEvents/exampleEvents');
var HandleCtrl = require('./exampleHandle/exampleHandle');
var IsContainerModelCtrl = require('./exampleIsContainerWithModel/exampleIsContainerWithModel');
var NameSpacesCtrl = require('./exampleNameSpaces/exampleNameSpaces');
var NestedNgRepeatCtrl = require('./exampleNestedNgRepeat/exampleNestedNgRepeat');
var NestedNgRepeatWithModelCtrl = require('./exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel');
var NgRepeatCtrl = require('./exampleNgRepeat/exampleNgRepeat');
var NgRepeatFilteredWithModelCtrl = require('./exampleNgRepeatFilteredWithModel/exampleNgRepeatFilteredWithModel');
var NgRepeatWithModelCtrl = require('./exampleNgRepeatWithModel/exampleNgRepeatWithModel');
var RemoveOnSpillCtrl = require('./exampleRemoveOnSpill/exampleRemoveOnSpill');
var RemoveOnSpillWithModelCtrl = require('./exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel');
var RevertOnSpillCtrl = require('./exampleRevertOnSpill/exampleRevertOnSpill.js');
var ScrollingDragCtrl = require('./exampleScrollingDrag/exampleScrollingDrag.js');
require('./templates');

/**
 *  Module Example App
 *
 *  DEMO app for dragular https://github.com/luckylooke/dragular
 */

angular
  .module('examplesApp', [dragular, 'templates', 'ui.router'])
  .config(examplesRouter)
  .controller('Basic', BasicCtrl)
  .controller('BasicModel', BasicModelCtrl)
  .controller('BoundingBox', BoundingBoxCtrl)
  .controller('BoundingBoxLockX', BoundingBoxLockXCtrl)
  .controller('BoundingBoxLockY', BoundingBoxLockYCtrl)
  .controller('Copy', CopyCtrl)
  .controller('CopyModel', CopyModelCtrl)
  .controller('CustomClasses', CustomClassesCtrl)
  .controller('DifferentOptionsModel', DifferentOptionsModelCtrl)
  .controller('Directive', DirectiveCtrl)
  .controller('DirectiveModel', DirectiveModelCtrl)
  .controller('DragOverEvents', DragOverEventsCtrl)
  .controller('Events', EventsCtrl)
  .controller('Handle', HandleCtrl)
  .controller('IsContainerModel', IsContainerModelCtrl)
  .controller('NameSpaces', NameSpacesCtrl)
  .controller('NestedNgRepeat',  NestedNgRepeatCtrl)
  .controller('NestedNgRepeatWithModel', NestedNgRepeatWithModelCtrl)
  .controller('NgRepeat', NgRepeatCtrl)
  .controller('NgRepeatFilteredWithModel', NgRepeatFilteredWithModelCtrl)
  .controller('NgRepeatWithModel', NgRepeatWithModelCtrl)
  .controller('RemoveOnSpill', RemoveOnSpillCtrl)
  .controller('RemoveOnSpillWithModel', RemoveOnSpillWithModelCtrl)
  .controller('RevertOnSpill', RevertOnSpillCtrl)
  .controller('ScrollingDrag', ScrollingDragCtrl)
  .controller('ExAppCtrl', ['$scope', function($scope) {
    $scope.examplesList = [{
        template: 'docsInstall/docsInstall.html',
        link: 'docsInstall',
        title: 'Installation'
    },{
        template: 'exampleBasic/exampleBasic.html',
        link: 'exampleBasic',
        title: 'Basic'
    },{
        template: 'exampleBasicWithModel/exampleBasicWithModel.html',
        link: 'exampleBasicWithModel',
        title: 'Basic - with model'
    },{
        template: 'exampleDifferentOptionsWithModel/exampleDifferentOptionsWithModel.html',
        link: 'exampleDifferentOptionsWithModel',
        title: 'Different options - with model'
    }, {
        template: 'exampleDirective/exampleDirective.html',
        link: 'exampleDirective',
        title: 'Directive'
    }, {
        template: 'exampleDirectiveWithModel/exampleDirectiveWithModel.html',
        link: 'exampleDirectiveWithModel',
        title: 'Directive - with model'
    }, {
        template: 'exampleEvents/exampleEvents.html',
        link: 'exampleEvents',
        title: 'Events'
    }, {
        template: 'exampleRemoveOnSpill/exampleRemoveOnSpill.html',
        link: 'exampleRemoveOnSpill',
        title: 'Remove on spill'
    }, {
        template: 'exampleRemoveOnSpillWithModel/exampleRemoveOnSpillWithModel.html',
        link: 'exampleRemoveOnSpillWithModel',
        title: 'Remove on spill - with model'
    }, {
        template: 'exampleRevertOnSpill/exampleRevertOnSpill.html',
        link: 'exampleRevertOnSpill',
        title: 'Revert on spill'
    }, {
        template: 'exampleCopy/exampleCopy.html',
        link: 'exampleCopy',
        title: 'Copy'
    }, {
        template: 'exampleCopyWithModel/exampleCopyWithModel.html',
        link: 'exampleCopyWithModel',
        title: 'Copy - with model'
    }, {
        template: 'exampleHandle/exampleHandle.html',
        link: 'exampleHandle',
        title: 'Handle'
    }, {
        template: 'exampleIsContainerWithModel/exampleIsContainerWithModel.html',
        link: 'exampleIsContainerWithModel',
        title: 'isContainer - with model'
    }, {
        template: 'exampleCustomClasses/exampleCustomClasses.html',
        link: 'exampleCustomClasses',
        title: 'Custom classes'
    }, {
        template: 'exampleNameSpaces/exampleNameSpaces.html',
        link: 'exampleNameSpaces',
        title: 'NameSpaces'
    }, {
        template: 'exampleDragOverEvents/exampleDragOverEvents.html',
        link: 'exampleDragOverEvents',
        title: 'Drag-over events'
    }, {
        template: 'exampleBoundingBox/exampleBoundingBox.html',
        link: 'exampleBoundingBox',
        title: 'BoundingBox'
    }, {
        template: 'exampleBoundingBoxLockX/exampleBoundingBoxLockX.html',
        link: 'exampleBoundingBoxLockX',
        title: 'BoundingBox + LockX'
    }, {
        template: 'exampleBoundingBoxLockY/exampleBoundingBoxLockY.html',
        link: 'exampleBoundingBoxLockY',
        title: 'BoundingBox + LockY'
    }, {
        template: 'exampleNgRepeat/exampleNgRepeat.html',
        link: 'exampleNgRepeat',
        title: 'ngRepeat'
    }, {
        template: 'exampleNgRepeatWithModel/exampleNgRepeatWithModel.html',
        link: 'exampleNgRepeatWithModel',
        title: 'ngRepeat - with model'
    }, {
        template: 'exampleNgRepeatFilteredWithModel/exampleNgRepeatFilteredWithModel.html',
        link: 'exampleNgRepeatFilteredWithModel',
        title: 'Filtered ngRepeat - with model'
    }, {
        template: 'exampleNestedNgRepeat/exampleNestedNgRepeat.html',
        link: 'exampleNestedNgRepeat',
        title: 'Nested ngRepead'
    }, {
        template: 'exampleNestedNgRepeatWithModel/exampleNestedNgRepeatWithModel.html',
        link: 'exampleNestedNgRepeatWithModel',
        title: 'Nested ngRepead - with model'
    }, {
        template: 'exampleScrollingDrag/exampleScrollingDrag.html',
        link: 'exampleScrollingDrag',
        title: 'Scrolling drag'
    }];

    $scope.highlightCode = function () {
        if(document.getElementsByTagName('code').length){
            var codeBlocks = document.getElementsByTagName('code');
            for (var i = codeBlocks.length - 1; i >= 0; i--) {
                hljs.highlightBlock(codeBlocks[i]);
            }
        }
    };

    var rowOffcanvas;
    $scope.toggleSidebar = function toggleSidebar () {
        if(!rowOffcanvas){
            rowOffcanvas = angular.element(document.getElementById('rowOffcanvas'));
        }
        rowOffcanvas.toggleClass('active');
    };

  }]);
