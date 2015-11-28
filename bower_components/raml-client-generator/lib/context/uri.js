/**
 * Sanitize all uris.
 *
 * @param  {String} uri
 * @param  {Object} spec
 * @return {String}
 */
module.exports = function (uri, spec) {
  return spec.format.uri(uri || '').replace(/\/+$/, '');
};
