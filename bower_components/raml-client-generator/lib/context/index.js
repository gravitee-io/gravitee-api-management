var extend                = require('extend');
var sanitizeUri           = require('./uri');
var sanitizeSecurity      = require('./security');
var sanitizeResources     = require('./resources');
var sanitizeParameters    = require('./parameters');
var sanitizeDocumentation = require('./documentation');

/**
 * Track indexes of each generated id.
 *
 * @type {Object}
 */
var PARAM_IDS;

/**
 * Default formatting options.
 *
 * @type {Object}
 */
var DEFAULT_FORMAT = {
  /**
   * Replace with your preferred uri templating language.
   *
   * @param  {String} uri
   * @return {String}
   */
  uri: function (uri) {
    return uri;
  },
  /**
   * Variable formatting support. By default, we'll just throw an error.
   */
  variable: function () {
    throw new Error('No variable format specified');
  },
  /**
   * Generate unique ids each spec iteration.
   */
  uniqueId: function (prefix) {
    var id = ++PARAM_IDS[prefix] || (PARAM_IDS[prefix] = 0);

    return prefix + id;
  }
};

/**
 * Validate (and sanitize) the passed in spec object.
 *
 * @param  {Obejct} spec
 * @return {Obejct}
 */
var validateSpec = function (spec) {
  // Reset the id generations.
  PARAM_IDS = {};

  spec.format = extend({}, DEFAULT_FORMAT, spec.format);

  return extend({}, spec);
};

/**
 * Flatten the resources object tree into an array.
 *
 * @param  {Object} resources
 * @return {Array}
 */
var flattenResources = function (resources) {
  var array = [];

  // Recursively push all resources into a single flattened array.
  (function recurse (resource) {
    array.push(resource);

    Object.keys(resource.children).forEach(function (key) {
      recurse(resource.children[key]);
    });
  })(resources);

  return array;
};

/**
 * Flatten the resources object into an array of methods.
 *
 * @param  {Object} resources
 * @return {Array}
 */
var flattenMethods = function (resources) {
  var array = [];

  // Recursively push all methods into a single flattened array.
  (function recurse (resource) {
    if (resource.methods) {
      Object.keys(resource.methods).forEach(function (key) {
        array.push(resource.methods[key]);
      });
    }

    Object.keys(resource.children).forEach(function (key) {
      recurse(resource.children[key]);
    });
  })(resources);

  return array;
};

/**
 * Create a context object for the templates to use during compilation.
 *
 * @param  {Object} ast
 * @param  {Object} spec
 * @return {Object}
 */
module.exports = function (ast, spec) {
  // Validate the spec before using.
  spec = validateSpec(spec);

  // Create an empty context object.
  var context = {
    id:                spec.format.uniqueId('client'),
    title:             ast.title || 'API Client',
    version:           ast.version,
    baseUri:           sanitizeUri(ast.baseUri, spec),
    security:          sanitizeSecurity(ast.securitySchemes, spec),
    resources:         sanitizeResources(ast.resources, spec),
    baseUriParameters: sanitizeParameters(ast.baseUriParameters, spec),
    documentation:     sanitizeDocumentation(ast.documentation)
  };

  context.allMethods       = flattenMethods(context.resources);
  context.allResources     = flattenResources(context.resources);
  context.supportedMethods = require('methods');

  return context;
};
