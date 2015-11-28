/**
 * Sanitize all documentation.
 *
 * @param  {Object} documentation
 * @return {Array}
 */
module.exports = function (documentation) {
  return Array.isArray(documentation) ? documentation.slice() : undefined
};