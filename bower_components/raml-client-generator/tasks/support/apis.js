var fs   = require('fs');
var path = require('path');

/**
 * Path the raml directory.
 *
 * @type {String}
 */
var RAML_DIR = path.join(__dirname, '../../test/fixtures/raml');

/**
 * An array of apis to automatically generate clients from.
 *
 * @type {Array}
 */
module.exports = fs.readdirSync(RAML_DIR)
  .filter(function (filename) {
    return path.extname(filename) === '.raml';
  })
  .map(function (filename) {
    return path.join(RAML_DIR, filename);
  });
