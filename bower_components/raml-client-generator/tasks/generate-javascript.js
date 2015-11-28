var gulp     = require('gulp');
var Bluebird = require('bluebird');
var exec     = require('./support/exec');
var generate = require('./support/generate');

/**
 * Generate the JavaScript API clients and install dependencies.
 */
gulp.task('generate:javascript', function () {
  return generate('javascript').then(function (clients) {
    var npmInstall = clients.map(function (output) {
      return exec('cd ' + output + ' && npm install');
    });

    return Bluebird.all(npmInstall);
  });
});
