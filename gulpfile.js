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
const gulp = require('gulp');
const HubRegistry = require('gulp-hub');
const browserSync = require('browser-sync');
const tslint = require("gulp-tslint");
const ts = require('gulp-typescript');
const conf = require('./conf/gulp.conf');

// Load some files into the registry
const hub = new HubRegistry([conf.path.tasks('*.js')]);

// Tell gulp to use the tasks just loaded
gulp.registry(hub);

gulp.task('build', gulp.series(gulp.parallel('other', 'webpack:dist')));
gulp.task('serve', gulp.series('webpack:watch', 'watch', 'browsersync'));
gulp.task('serve:demo', gulp.series('webpack:watch', 'watch', 'browsersync:demo'));
gulp.task('serve:nightly', gulp.series('webpack:watch', 'watch', 'browsersync:nightly'));
gulp.task('serve:dist', gulp.series('default'));
gulp.task('default', gulp.series('clean', 'build'));
gulp.task('watch', watch);
gulp.task('lint', lint);
gulp.task('lint:fix', lintFix);
gulp.task('compile', compile);

function compile() {
  var tsProject = ts.createProject('tsconfig.json');
  return gulp.src('src/**/*.ts')
    .pipe(tsProject())
}

function reloadBrowserSync(cb) {
  browserSync.reload();
  cb();
}

function watch(done) {
  gulp.watch(conf.path.tmp('index.html'), reloadBrowserSync);
  done();
}

function lint() {
  return gulp.src('src/**/*.ts')
    .pipe(tslint())
    .pipe(tslint.report({
      summarizeFailureOutput: true
    }));
}

function lintFix() {
  return gulp.src('src/**/*.ts')
    .pipe(tslint({
      fix: true
    }))
    .pipe(tslint.report({
      summarizeFailureOutput: true
    }));
}
