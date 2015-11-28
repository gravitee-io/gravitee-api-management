var path     = require('path');
var Bluebird = require('bluebird');
var apis     = require('./apis');
var exec     = require('./exec');

/**
 * Path to the client test directory.
 *
 * @type {String}
 */
var CLIENT_TEST_DIR = path.join(__dirname, '../../test/clients');

/**
 * Path to the client generator command line script.
 *
 * @type {String}
 */
var RAML_CLIENT_SCRIPT = path.join(__dirname, '../../bin/raml-client.js');

/**
 * Return a function that can be used with gulp for generating language
 * specific api clients for testing.
 *
 * @param  {String}   language
 * @return {Function}
 */
module.exports = function (language) {
  var clients = apis.map(function (filename) {
    var name   = path.basename(filename, '.raml');
    var output = path.join(CLIENT_TEST_DIR, language, '.tmp', name);

    // Create the command to run.
    var cmd = [
      'node',
      RAML_CLIENT_SCRIPT,
      '--entry ' + filename,
      '--output ' + output,
      '--language ' + language
    ].join(' ');

    // Create the client using the command line interface.
    return exec(cmd)
      .spread(function (stdout, stderr) {
        process.stdout.write(stdout);
        process.stderr.write(stderr);
      })
      .return(output);
  });

  return Bluebird.all(clients);
};
