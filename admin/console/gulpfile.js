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
var gulp = require('gulp'),
  concat = require('gulp-concat'),
  rename = require('gulp-rename'),
  shell = require('gulp-shell'),
  traceur = require('gulp-traceur'),
  webserver = require('gulp-webserver');

// run init tasks
gulp.task('build', ['dependencies', 'angular2', 'js', 'html', 'css']);

// serve the build dir
gulp.task('serve', ['build', 'watch'], function () {
  gulp.src('build')
    .pipe(webserver({
      livereload: true,
      open: true
    }));
});

// watch for changes and run the relevant task
gulp.task('watch', function () {
  gulp.watch('src/**/*.js', ['js']);
  gulp.watch('src/**/*.html', ['html']);
  gulp.watch('src/**/*.css', ['css']);
});

// move dependencies into build dir
gulp.task('dependencies', function () {
  return gulp.src([
    'node_modules/angular2/node_modules/rx/dist/rx.js',
    'node_modules/angular2/node_modules/traceur/bin/traceur.js',
    'node_modules/angular2/node_modules/traceur/bin/traceur-runtime.js',
    'node_modules/angular2/node_modules/zone.js/dist/zone.js',
    'node_modules/es6-module-loader/dist/es6-module-loader.js',
    'node_modules/es6-module-loader/dist/es6-module-loader.js.map',
    'node_modules/reflect-metadata/Reflect.js',
    'node_modules/systemjs/dist/system.js',
    'node_modules/systemjs/dist/system.js.map'
  ])
    .pipe(gulp.dest('build/lib'));
});

// tanspile, concat & move angular
gulp.task('angular2', function () {
  return gulp.src([
    traceur.RUNTIME_PATH,
    'node_modules/angular2/es6/prod/*.es6',
    'node_modules/angular2/es6/prod/src/**/*.es6'
  ], {
    base: 'node_modules/angular2/es6/prod'
  })
    .pipe(rename(function (path) {
      path.dirname = 'angular2/' + path.dirname;
      path.extname = '';
    }))
    .pipe(traceur({
      modules: 'instantiate',
      moduleName: true
    }))
    .pipe(concat('angular2.js'))
    .pipe(gulp.dest('build/lib'));
});

// transpile & move js
gulp.task('js', function () {
  return gulp.src('src/**/*.js')
    .pipe(rename({
      extname: ''
    }))
    .pipe(traceur({
      modules: 'instantiate',
      moduleName: true,
      annotations: true,
      types: true
    }))
    .pipe(rename({
      extname: '.js'
    }))
    .pipe(gulp.dest('build'));
});

// move html
gulp.task('html', function () {
  return gulp.src('src/**/*.html')
    .pipe(gulp.dest('build'))
});

// move css
gulp.task('css', function () {
  return gulp.src('src/**/*.css')
    .pipe(gulp.dest('build'));
});

gulp.task('test', function () {
});
