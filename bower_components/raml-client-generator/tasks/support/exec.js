var exec     = require('child_process').exec;
var Bluebird = require('bluebird');

/**
 * Promisified child process exec.
 *
 * @type {Function}
 */
module.exports = Bluebird.promisify(exec);
