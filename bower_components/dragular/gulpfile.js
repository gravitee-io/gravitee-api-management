'use strict';
var gulp = require('gulp');
var gutil = require('gulp-util');
var gulpif = require('gulp-if');
var browserify = require('browserify');
var watchify = require('watchify');
var buffer = require('vinyl-buffer');
var uglify = require('gulp-uglify');
var size = require('gulp-size');
var source = require('vinyl-source-stream');
var sequence = require('run-sequence');
var rename = require('gulp-rename');
var minifyCss = require('gulp-minify-css');
var autoprefixer = require('gulp-autoprefixer');
var browserSync = require('browser-sync');
var jshint = require('gulp-jshint');
var sourcemaps = require('gulp-sourcemaps');
var concat = require('gulp-concat');
var ngAnnotate = require('browserify-ngannotate');
var templateCache = require('gulp-angular-templatecache');
var ghPages = require('gulp-gh-pages');
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
  // Predefined browserify configs to keep tasks DRY
  browserify: {
    dragular: {
      type: 'dragular',
      entryPoint: './src/dragularModule.js',
      bundleName: 'dragular.js',
      dest: './dist',
    },
    docs: {
      type: 'docs',
      entryPoint: './docs/src/examples/examplesApp.js',
      bundleName: 'examples.js',
      dest: './docs/dist'
    }
  },
  // A flag attribute to switch modes.
  isProd: false
};

/*
* browserifyDefaults stores current browserify settings (like entry point or
* output directory). This metadata is used to configure the scripts compilation
* process.
*/
var browserifyDefaults = config.browserify.dragular;

function handleErrors(err) {
  gutil.log(err.toString());
  this.emit('end');
}

/*
* See http://blog.avisi.nl/2014/04/25/how-to-keep-a-fast-build-with-browserify-and-reactjs/
*/
function buildScript() {

  var bundler = browserify({
    entries: browserifyDefaults.entryPoint,
    debug: false,
    cache: {},
    packageCache: {},
    fullPaths: false
  }, watchify.args);

  var transforms = [
    'brfs',
    'bulkify',
    ngAnnotate
  ];

  // Watch files for changes and only rebuilds what it needs to
  if (!config.isProd) {
    bundler = watchify(bundler);
    bundler.on('update', function() {
      lintAndRebundle();
    });
  }

  // Apply browserify transformations
  transforms.forEach(function(transform) {
    bundler.transform(transform);
  });

  function rebundle() {
    var stream = bundler.bundle();

    return stream.on('error', handleErrors)
      .pipe(source(browserifyDefaults.bundleName))
      .pipe(buffer())
      .pipe(gulpif(config.isProd, gulp.dest(browserifyDefaults.dest)))
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
      .pipe(gulp.dest(browserifyDefaults.dest))
      .pipe(gulpif(browserSync.active, browserSync.stream()));
  }

  function lintAndRebundle() {
    if (browserifyDefaults.type === 'dragular') {
      return sequence('lint', rebundle);
    }

    return sequence('lint:docs', rebundle);
  }

  return lintAndRebundle();
}

gulp.task('browserify', function() {
  return buildScript();
});

gulp.task('styles', function() {

  return gulp.src(config.dragular.styles)
    .pipe(autoprefixer({
      browsers: [ 'last 15 versions', '> 1%', 'ie 8', 'ie 7' ],
      cascade: false
    }))
    .pipe(concat('dragular.css'))
    .pipe(gulpif(config.isProd, gulp.dest(config.dragular.dest)))
    .pipe(gulpif(config.isProd, minifyCss()))
    .pipe(gulpif(config.isProd, rename({
      suffix: '.min'
    })))
    .pipe(size({
      title: 'Styles: '
    }))
    .pipe(gulp.dest(config.dragular.dest))
    .pipe(gulpif(browserSync.active, browserSync.stream()));
});

gulp.task('styles:docs', function() {

  return gulp.src([
    config.docs.styles,
    config.dragular.styles
  ])
  .pipe(autoprefixer({
    browsers: [ 'last 15 versions', '> 1%', 'ie 8', 'ie 7' ],
    cascade: false
  }))
  .pipe(concat('examples.css'))
  .pipe(gulp.dest(config.docs.dest))
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
    
    port: process.env.PORT || config.browserSync.port, // cloud9 improvement
    server: {
      baseDir: config.browserSync.server,
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
      standalone: true,
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
  gulp.watch(config.docs.styles,  ['styles:docs']);
  gulp.watch(config.docs.templates,  ['templates:docs']);
  gulp.watch(config.docs.index, browserSync.reload);
  gulp.watch('./CONTRIBUTING.md', ['markdown']);
});

/*
* Launch browserSync server, watch & automatically refresh connected browsers
* on changes, generate non-minified but concatenated output.
*/
gulp.task('dev', function() {
  config.isProd = false;
  browserifyDefaults = config.browserify.dragular;

  sequence(['browserify', 'styles'], 'watch');
});

gulp.task('dev:docs', function() {
  config.isProd = false;
  browserifyDefaults = config.browserify.docs;

  sequence('markdown', 'templates:docs', ['browserify', 'styles:docs'], 'watch:docs');
});

/*
* Generate production ready minified and concantenated output.
*/
gulp.task('build', function() {
  config.isProd = true;
  browserifyDefaults = config.browserify.dragular;

  sequence(['browserify', 'styles'], 'build:docs');
});

gulp.task('build:docs', function() {
  config.isProd = true;
  browserifyDefaults = config.browserify.docs;

  sequence('markdown', 'templates:docs', ['browserify', 'styles']);
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
