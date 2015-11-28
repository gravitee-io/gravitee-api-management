/**
 * Pull out request parameters from the resource.
 *
 * @param  {Object} resource
 * @return {String}
 */
var params = function (resource) {
  return resource.uriParameters.map(function (param) {
    return param.displayName;
  }).join(', ');
};

/**
 * Stringify a resource into a request snippet.
 *
 * @param  {Object} resource
 * @return {String}
 */
module.exports = function (resource) {
  var parts = [];
  var part  = resource;

  while (part && part.parent) {
    var segment = part.key;

    // If uri parameters exist, push onto the stack.
    if (part.uriParameters.length) {
      segment += '(' + params(part) + ')';
    }

    parts.unshift(segment);

    part = part.parent;
  }

  return 'resources' + (parts.length ? '.' + parts.join('.') : '');
};
