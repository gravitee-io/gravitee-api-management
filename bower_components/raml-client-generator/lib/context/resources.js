var sanitizeUri        = require('./uri');
var sanitizeMethods    = require('./methods');
var sanitizeParameters = require('./parameters');

/**
 * Match template tags in a string.
 *
 * @type {RegExp}
 */
var TEMPLATE_REGEXP = /\{[^\{\}]+\}/g;

/**
 * Convert a URI parameter into a variable name.
 *
 * @param  {String} uri
 * @param  {Object} spec
 * @return {String}
 */
var toPropertyFormat = function (uri, spec) {
  // Replace the prefixed path segment.
  uri = uri.replace(/^[\.\/]/, '');

  // Handle a only a single parameter. E.g. "/{param}".
  if (/^\{[^\{\}]+\}$/.test(uri)) {
    return spec.format.variable(uri.slice(1, -1));
  }

  // Handle static text with trailing parameters. E.g. "/string{id}".
  if (/^[^\{\}]+(?:\{[^\{\}]+\})*$/.test(uri)) {
    return spec.format.variable(uri.replace(/\{.+\}$/, ''));
  }
};

/**
 * Convert the uri to the language replacement format.
 *
 * @param  {String} uri
 * @param  {Object} spec
 * @return {String}
 */
var toUriFormat = function (uri, spec) {
  var index = 0;

  // Index the template parameters instead of having unique names. This
  // saves a minor amount of space and makes processing *a lot* easier.
  var indexedUri = uri.replace(/\{[^\{\}]+\}/g, function () {
    return '{' + (index++) + '}';
  });

  return sanitizeUri(indexedUri, spec);
};

/**
 * Attach the current resource to the object structure.
 *
 * @param {Object} obj
 * @param {Object} resource
 * @param {Object} spec
 */
var attachResource = function (obj, resource, spec) {
  // Split the relative uri into the valid parts. This includes special-case
  // handling for the `mediaTypeExtension` parameter according to the spec.
  var uriParts = resource.relativeUri.split(
    /(?=\.|\/|\{mediaTypeExtension\}$)/
  );

  // Sanitize the uri parameters before extracting.
  var parameters = sanitizeParameters(resource.uriParameters);

  /**
   * Return the parameter from the parameters object based on the passed in tag.
   *
   * @param  {String} param
   * @return {Object}
   */
  var getParamTag = function (param) {
    return parameters[param.slice(1, -1)];
  };

  // Iterate over each of the uri parts and nest on the root object.
  (function recurse (obj, parts) {
    var part = parts[0];

    // Add a period to the beginning of the manual media type extension.
    if (part === '{mediaTypeExtension}' && parts.length === 1) {
      if (Array.isArray(parameters.mediaTypeExtension.enum)) {
        parameters.mediaTypeExtension.enum.forEach(function (extension) {
          extension = '.' + extension.replace(/^\./, '');

          return recurse(obj, [extension]);
        });
      }

      // Remove the media type enum from documentation purposes.
      delete parameters.mediaTypeExtension.enum;

      part = '.{mediaTypeExtension}';
    }

    // Recursively create (or select) the child resource. If the resource is
    // a single slash, it should be attached to the same object.
    if (part !== '/') {
      // Convert the uri part into a variable name.
      var key = toPropertyFormat(part, spec);

      // TODO: Handle duplicate variable names and missing variable names.
      if (!key || obj.children.hasOwnProperty(key)) {
        // Handle exact matching relative uris. If they don't match exactly,
        // it's impossible for us to handle generically.
        if (part === obj.children[key].relativeUri) {
          obj = obj.children[key];
        } else {
          return;
        }
      } else {
        var matches       = part.match(TEMPLATE_REGEXP) || [];
        var uriParameters = Array.prototype.map.call(matches, getParamTag);

        obj = obj.children[key] = {
          id:            spec.format.uniqueId('resource'),
          key:           key,
          parent:        obj,
          children:      {},
          relativeUri:   toUriFormat(part, spec),
          uriParameters: uriParameters
        };
      }
    }

    // Recursively append uri parts.
    if (parts.length > 1) {
      return recurse(obj, parts.slice(1));
    }

    obj.methods     = sanitizeMethods(resource.methods, obj, spec);
    obj.description = (resource.description || '').trim();

    if (resource.resources) {
      resource.resources.forEach(function (resource) {
        attachResource(obj, resource, spec);
      });
    }

    // TODO: Implement node backtracking when no methods are available.

    return obj;
  })(obj, uriParts);
};

/**
 * Sanitize resources into nested object form.
 *
 * @param  {Object} resources
 * @param  {Object} spec
 * @return {Object}
 */
module.exports = function (resources, spec) {
  // Create the root resource object for consistency of behaviour.
  var obj = {
    id:            spec.format.uniqueId('resource'),
    children:      {},
    relativeUri:   '',
    uriParameters: []
  };

  if (!resources) {
    return obj;
  }

  // Iterate over the resources array and attach each .
  resources.forEach(function (resource) {
    return attachResource(obj, resource, spec);
  });

  return obj;
};
