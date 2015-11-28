/* global angular */
'use strict';

var bulk = require('bulk-require');

/**
 * Dragular 3.2.0 by Luckylooke https://github.com/luckylooke/dragular
 * Angular version of dragula https://github.com/bevacqua/dragula
 */
module.exports = angular.module('dragularModule', []);

bulk(__dirname, ['./**/!(*Module).js']);
