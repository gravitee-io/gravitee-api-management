/* global process */
'use strict';
var gulp = require('gulp');
var gutil = require('gulp-util');
var gulpif = require('gulp-if');
var webpack = require('webpack-stream');
var size = require('gulp-size');
var sequence = require('run-sequence');
var uglify = require('gulp-uglify');
var rename = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var autoprefixer = require('gulp-autoprefixer');
var browserSync = require('browser-sync');
var jshint = require('gulp-jshint');
var concat = require('gulp-concat');
var templateCache = require('gulp-angular-templatecache');
var ghPages = require('gulp-gh-pages');
var sourcemaps = require('gulp-sourcemaps');
var markdown = require('gulp-markdown');

var config = {
  dragular: {
    scripts: './src/*.js',
    styles: './src/*.css',
    dest: './dist'
  },
  docs: {
    src: './docs/src/examples',
    index: './docs/index.html',
    scripts: './docs/src/examples/**/*.js',
    styles: './docs/src/**/*.css',
    templates: './docs/src/examples/**/*.html',
    dest: './docs/dist'
  },
  browserSync: {
    port: '3000',
    server: './docs'
  },
  // Predefined scripts & styles configs to keep tasks DRY
  scripts: {
    libraryTarget: 'umd',
    dragular: {
      type: 'dragular',
      entryPoint: './src/dragularModule.js',
      bundleName: 'dragular.js',
      dest: './dist'
    },
    docs: {
      type: 'docs',
      entryPoint: './docs/src/examples/examplesApp.js',
      bundleName: 'examples.js',
      dest: './docs/dist'
    }
  },
  styles: {
    dragular: {
      entryPoint: './src/*.css',
      bundleName: 'dragular.css',
      dest: './dist'
    },
    docs: {
      entryPoint: [
        './src/*.css',
        './docs/src/**/*.css'
      ],
      bundleName: 'examples.css',
      dest: './docs/dist'
    }
  },
  // A flag attribute to switch modes
  isProd: false
};

/*
* Defaults stores temporary scripts & styles settings (like entry point or
* output directory). This metadata is used to configure the webpack compilation
* process.
*/
var defaults = {
  scripts: config.scripts.dragular,
  styles: config.styles.dragular
}

function handleErrors(err) {
  gutil.log(err.toString());
  this.emit('end');
}

/*
* See http://blog.avisi.nl/2014/04/25/how-to-keep-a-fast-build-with-browserify-and-reactjs/
*/
function buildScript() {

  function rebundle() {
    return gulp.src(defaults.scripts.entryPoint)
      .on('error', handleErrors)
      .pipe(webpack({
        watch: config.isProd ? false : true,
        output: {
          libraryTarget: config.scripts.libraryTarget,
          filename: defaults.scripts.bundleName
        }
      }))
      .pipe(gulpif(config.isProd, gulp.dest(defaults.scripts.dest)))
      .pipe(gulpif(config.isProd, size({
        title: 'Non-minified scripts: '
      })))
      .pipe(gulpif(config.isProd, sourcemaps.init({loadMaps: true})))
      .pipe(gulpif(config.isProd, uglify({
        compress: { drop_console: true }
      })))
      .pipe(gulpif(config.isProd, rename({
        suffix: '.min'
      })))
      .pipe(size({
        title: 'Scripts: '
      }))
      .pipe(gulpif(config.isProd, sourcemaps.write('./')))
      .pipe(gulp.dest(defaults.scripts.dest))
      .pipe(gulpif(browserSync.active, browserSync.stream()));
  }

  function lintAndRebundle() {
    if (defaults.scripts.type === 'dragular') {
      return sequence('lint', rebundle);
    }

    return sequence('lint:docs', rebundle);
  }

  return lintAndRebundle();
}

gulp.task('scripts', function() {
  return buildScript();
});

gulp.task('styles', function() {

  return gulp.src(defaults.styles.entryPoint)
    .pipe(autoprefixer({
      browsers: [ 'last 15 versions', '> 1%', 'ie 8', 'ie 7' ],
      cascade: false
    }))
    .pipe(concat(defaults.styles.bundleName))
    .pipe(gulpif(config.isProd, gulp.dest(defaults.styles.dest)))
    .pipe(gulpif(config.isProd, size({
      title: 'Non-minified styles:  '
    })))
    .pipe(gulpif(config.isProd, minifyCss()))
    .pipe(gulpif(config.isProd, rename({
      suffix: '.min'
    })))
    .pipe(size({
      title: 'Styles: '
    }))
    .pipe(gulp.dest(defaults.styles.dest))
    .pipe(gulpif(browserSync.active, browserSync.stream()));
});

gulp.task('lint', function() {

  return gulp.src([config.dragular.scripts])
    .pipe(jshint())
    .pipe(jshint.reporter('jshint-stylish'));
});

gulp.task('lint:docs', function() {

  return gulp.src([config.docs.scripts, '!./docs/src/examples/templates.js'])
    .pipe(jshint())
    .pipe(jshint.reporter('jshint-stylish'));
});

gulp.task('serve', function () {

  browserSync({
    ui:{
      port: '8081'
    },
    port: (typeof process !== 'undefined' && process.env.PORT) || config.browserSync.port, // cloud9 improvement
    host: typeof process !== 'undefined' && process.env.IP, // cloud9 improvement
    server: {
      baseDir: config.browserSync.server
    },
    logConnections: true,
    logFileChanges: true,
    notify: true
  });
});

/*
* Concatenate and register templates in the $templateCache. The resulting file
* (templates.js) is placed inside the directory specified in config.docs.src.
*/
gulp.task('templates:docs', function() {

  return gulp.src(config.docs.templates)
    .pipe(templateCache({
      moduleSystem: 'Browserify',
      standalone: true
    }))
    .pipe(gulp.dest(config.docs.src));
});

/*
* Watch files for changes
*/
gulp.task('watch', ['serve'], function() {
  gulp.watch(config.dragular.styles,  ['styles']);
});

gulp.task('watch:docs', ['serve'], function() {
  gulp.watch(config.docs.styles,  ['styles']);
  gulp.watch(config.docs.templates,  ['templates:docs']);
  gulp.watch(config.docs.index, browserSync.reload);
  gulp.watch('./CONTRIBUTING.md', ['markdown']);
});

/*
* Launch browserSync server, watch & automatically refresh connected browsers
* on changes, generate non-minified but concatenated output.
*/
gulp.task('dev', function() {
  config.isProd = true;
  defaults = {
    scripts: config.scripts.dragular,
    styles: config.styles.dragular
  };

  sequence(['scripts', 'styles'], 'watch');
});

gulp.task('dev:docs', function() {
  config.isProd = true;
  defaults = {
    scripts: config.scripts.docs,
    styles: config.styles.docs
  };

  sequence('markdown', 'templates:docs', ['scripts', 'styles'], 'watch:docs');
});

/*
* Generate production ready minified and concantenated output.
*/
gulp.task('build', function() {
  config.isProd = true;
  defaults = {
    scripts: config.scripts.dragular,
    styles: config.styles.dragular
  };

  sequence(['scripts', 'styles'], 'build:docs');
});

gulp.task('build:docs', function() {
  config.isProd = true;
  defaults = {
    scripts: config.scripts.docs,
    styles: config.styles.docs
  };

  sequence('markdown', 'templates:docs', ['scripts', 'styles']);
});

/*
* Publish code to GitHub pages.
*/
gulp.task('deploy:docs', function() {
  return gulp.src('./docs/**/*')
    .pipe(ghPages());
});

/*
* Compiles markdown to html.
*/
gulp.task('markdown', function () {

  return gulp.src('./CONTRIBUTING.md')
    .pipe(markdown())
    .pipe(gulpif('**/CONTRIBUTING.html', rename({basename: 'contribute'})))
    .pipe(gulp.dest(config.docs.src + '/partials/autogenerated'));
});
