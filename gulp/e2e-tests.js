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
'use strict';

var path = require('path');
var gulp = require('gulp');
var conf = require('./conf');

var browserSync = require('browser-sync');

var $ = require('gulp-load-plugins')();

// Downloads the selenium webdriver
gulp.task('webdriver-update', $.protractor.webdriver_update);

gulp.task('webdriver-standalone', $.protractor.webdriver_standalone);

function runProtractor (done) {
  var params = process.argv;
  var args = params.length > 3 ? [params[3], params[4]] : [];

  gulp.src(path.join(conf.paths.e2e, '/**/*.js'))
    .pipe($.protractor.protractor({
      configFile: 'protractor.conf.js',
      args: args
    }))
    .on('error', function (err) {
      // Make sure failed tests cause gulp to exit non-zero
      throw err;
    })
    .on('end', function () {
      // Close browser sync server
      browserSync.exit();
      done();
    });
}

gulp.task('protractor', ['protractor:src']);
gulp.task('protractor:src', ['serve:e2e', 'webdriver-update'], runProtractor);
gulp.task('protractor:dist', ['serve:e2e-dist', 'webdriver-update'], runProtractor);
