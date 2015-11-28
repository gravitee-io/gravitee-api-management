var pick = require('object.pick');

/**
 * Sanitize the parameter into a more reusable object.
 *
 * @param  {Object} parameter
 * @return {Object}
 */
var sanitizeParameter = function (parameter) {
  var obj = pick(parameter, [
    'displayName',
    'type',
    'enum',
    'pattern',
    'minLength',
    'maxLength',
    'minimum',
    'maximum',
    'example',
    'repeat',
    'required',
    'default'
  ]);

  obj.description = (parameter.description || '').trim();

  // Automatically set the default parameter from the enum value.
  if (obj.default == null && Array.isArray(obj.enum)) {
    obj.default = obj.enum[0];
  }

  return obj;
};

/**
 * Sanitize parameters into something more consumable.
 *
 * @param  {Object} parameters
 * @return {Object}
 */
module.exports = function (parameters) {
  var obj = {};

  if (!parameters) {
    return obj;
  }

  // Iterate over every parameter and generate a new parameters object.
  Object.keys(parameters).forEach(function (key) {
    obj[key] = sanitizeParameter(parameters[key]);
  });

  return obj;
};
