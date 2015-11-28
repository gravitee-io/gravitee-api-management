var gulp  = require('gulp');
var path  = require('path');
var karma = require('karma').server;
var spawn = require('child_process').spawn;
var argv  = require('minimist')(process.argv.slice(2));

/**
 * Path to the mocha executable.
 */
var MOCHA_PATH = path.join(__dirname, '../node_modules/mocha/bin/_mocha');

/**
 * Test the JavaScript clients in node.
 */
gulp.task('test:javascript:node', [
  'server',
  'generate:javascript'
], function () {
  var testProcess = spawn(MOCHA_PATH, [
    path.join(__dirname, '../test/clients/javascript/node/**/*.js'),
    '-R',
    'spec',
    '--require',
    path.join(__dirname, '../test/clients/javascript/support/globals.js')
  ]);

  testProcess.stdout.pipe(process.stdout);
  testProcess.stderr.pipe(process.stderr);

  return testProcess.stdout;
});

/**
 * Test the JavaScript clients in the browser.
 */
gulp.task('test:javascript:browser', [
  'server',
  'generate:javascript'
], function (done) {
  var opts = {
    singleRun: true,
    configFile: path.join(__dirname, 'support', 'karma.conf.js')
  };

  // If continuous integration is enabled, test only a single browser.
  if (argv.ci) {
    opts.browsers = ['PhantomJS'];
  }

  return karma.start(opts, done);
});

/**
 * Test the JavaScript clients in all environments.
 */
gulp.task('test:javascript', [
  'test:javascript:node',
  'test:javascript:browser'
]);
