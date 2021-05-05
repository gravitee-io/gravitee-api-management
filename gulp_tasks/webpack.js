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
const gutil = require('gulp-util');

const webpack = require('webpack');
const webpackDistConf = require('../conf/webpack-dist.conf');
const gulpConf = require('../conf/gulp.conf');

gulp.task('webpack:dist', (done) => {
  process.env.NODE_ENV = 'production';
  webpackWrapper(webpackDistConf, done);
});

function webpackWrapper(conf, done) {
  const webpackBundler = webpack(conf);

  const webpackChangeHandler = (err, stats) => {
    if (err) {
      gulpConf.errorHandler('Webpack')(err);
    }
    gutil.log(
      stats.toString({
        colors: true,
        chunks: false,
        hash: false,
        version: false,
      }),
    );
    if (done) {
      done();
      done = null;
    }
  };

  webpackBundler.run(webpackChangeHandler);
}
