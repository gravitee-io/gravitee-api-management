/**
 * Sanitize resources into nested object form.
 *
 * @param  {Object} securitySchemes
 * @return {Object}
 */
module.exports = function (securitySchemes) {
  var obj = {};

  if (!Array.isArray(securitySchemes)) {
    return obj;
  }

  securitySchemes.forEach(function (schemes) {
    Object.keys(schemes).forEach(function (key) {
      var scheme = schemes[key];

      obj[scheme.type] = scheme;
    });
  });

  return obj;
};
