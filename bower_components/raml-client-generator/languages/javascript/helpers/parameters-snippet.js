/**
 * Iterate over the parameters and turn into a string.
 *
 * @param  {Object} parameters
 * @return {String}
 */
module.exports = function (parameters) {
  return Object.keys(parameters).map(function (key) {
    var parameter = parameters[key];
    var title     = '* **' + parameter.displayName + '**';
    var options   = [];

    if (parameter.type) {
      options.push(parameter.type);
    }

    if (Array.isArray(parameter.enum) && parameter.enum.length) {
      options.push('one of (' + parameter.enum.join(', ') + ')');
    }

    if (parameter.default) {
      options.push('default: ' + parameter.default);
    }

    return title +
      (options.length ? ' _' + options.join(', ') + '_' : '') +
      (parameter.description ? '\n\n' + parameter.description : '');
  }).join('\n\n');
};
