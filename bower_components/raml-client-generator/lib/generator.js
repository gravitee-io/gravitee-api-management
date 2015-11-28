var extend        = require('extend');
var helpers       = require('./helpers');
var createContext = require('./context');

// Handlebars required in node to support `require('x.hbs')`.
require('handlebars');

/**
 * Compile an api client using a combination of the ast, spec and user data.
 *
 * @param  {Object} ast
 * @param  {Object} spec
 * @param  {Object} data
 * @return {Object}
 */
function generate (ast, spec, data) {
  // Handlebars compile options from the specification.
  var options = {
    data:     data,
    helpers:  extend({}, helpers, spec.helpers),
    partials: extend({}, spec.partials)
  };

  // Allow the language to override the file generator.
  var createFiles = spec.files || generateFiles;
  var context = createContext(ast, spec);
  var files = createFiles(spec.templates, context, options);

  // Create the compile object. We resolve this object instead of just the
  // files so that external utilities have access to the context object. For
  // example, the "API Notebook" project needs to add runtime documentation.
  return {
    files: files,
    context: context,
    options: options
  };
}

/**
 * Default file generator directly from templates.
 *
 * @param  {Object} templates
 * @param  {Object} context
 * @param  {Object} options
 * @return {Object}
 */
function generateFiles (templates, context, options) {
  var files = {};

  Object.keys(templates).forEach(function (key) {
    files[key] = templates[key](context, options);
  });

  return files;
}

/**
 * Generate a language specific client generator based on passed in spec.
 *
 * @param  {Object}   spec
 * @return {Function}
 */
module.exports = function (spec) {
  /**
   * Generate an API client by passed in an AST.
   *
   * @param  {Object} ast
   * @param  {Object} options
   * @return {Object}
   */
  return function (ast, options) {
    return generate(ast, spec, options);
  };
};
