/**
 * Map dependencies to their environment.
 *
 * @type {Object}
 */
var DEPS_MAP = {
  node: {
    popsicle:     'require(\'popsicle\')',
    ClientOAuth2: 'require(\'client-oauth2\')'
  },
  amd: {
    popsicle:     '\'popsicle\'',
    ClientOAuth2: '\'ClientOAuth2\''
  },
  browser: {
    popsicle:     'root.popsicle',
    ClientOAuth2: 'root.ClientOAuth2'
  }
};

/**
 * Map an array of dependency names to values.
 *
 * @param  {Array}  deps
 * @param  {String} env
 * @return {Array}
 */
function mapDeps (deps, env) {
  if (!env) {
    return deps;
  }

  return deps.map(function (dep) {
    return DEPS_MAP[env][dep];
  });
}

/**
 * Create a dependencies string.
 *
 * @param  {Object} context
 * @param  {String} env
 * @return {String}
 */
module.exports = function (context, env) {
  var deps = ['popsicle'];

  // OAuth 2.0 depends on ClientOAuth2 to work.
  if (context.security['OAuth 2.0']) {
    deps.push('ClientOAuth2');
  }

  // Returns an array of strings for AMD.
  if (env === 'amd') {
    return '[' + mapDeps(deps, env).join(', ') + ']';
  }

  return mapDeps(deps, env).join(', ');
};
