var indent = require('indent-string');
var trim   = String.prototype.trim;
var hasOwn = Object.prototype.hasOwnProperty;

/**
 * Case sanitization support.
 */
exports.camelCase    = require('camel-case');
exports.pascalCase   = require('pascal-case');
exports.constantCase = require('constant-case');
exports.paramCase    = require('param-case');
exports.lowerCase    = require('lower-case');
exports.upperCase    = require('upper-case');
exports.snakeCase    = require('snake-case');

/**
 * Formatting utilities.
 */
exports.indent = function (/* input, count, character, opts */) {
  var args      = Array.prototype.slice.call(arguments);
  var opts      = args.pop();
  var input     = (opts.fn ? opts.fn(this) : args[0]);
  var count     = (opts.fn ? args[0] : args[1]) || 2;
  var character = (opts.fn ? args[1] : args[2]) || ' ';

  return indent(input, character, count);
};

/**
 * The `||` conditional as a helper.
 *
 * @return {Boolean}
 */
exports.or = function (/* ...condition, opts */) {
  for (var i = 0; i < arguments.length - 1; i++) {
    if (arguments[i]) {
      return true;
    }
  }

  return false;
};

/**
 * The `&&` conditional as a helper.
 *
 * @return {Boolean}
 */
exports.and = function (/* ...condition, opts */) {
  for (var i = 0; i < arguments.length - 1; i++) {
    if (!arguments[i]) {
      return false;
    }
  }

  return true;
};

/**
 * The `!` conditional as a helper.
 *
 * @return {Boolean}
 */
exports.not = function (condition) {
  return !condition;
};

/**
 * Get the keys of an object.
 */
exports.keys = function (obj) {
  var keys = [];

  for (var prop in obj) {
    if (hasOwn.call(obj, prop)) {
      keys.push(prop);
    }
  }

  return keys;
};

/**
 * Return all the nested by the name.
 *
 * @param  {Object} obj
 * @param  {String} name
 * @return {Array}
 */
exports.pluck = function (obj, name) {
  return Object.keys(obj).map(function (key) {
    return obj[key] && obj[key][name];
  });
};

/**
 * Turn an array of keys into an object.
 *
 * @param  {Array}  keys
 * @param  {Array}  values
 * @return {Object}
 */
exports.object = function (keys, values) {
  var obj = {};

  for (var i = 0; i < keys.length; i++) {
    obj[keys[i]] = values[i];
  }

  return obj;
};

/**
 * Check whether a value is contained within an array.
 *
 * @param  {Array}   array
 * @param  {*}       value
 * @return {Boolean}
 */
exports.contains = function (array, value) {
  return array.indexOf(value) > -1;
};

/**
 * Debugging.
 */
exports.log = function () {
  var opts = arguments[arguments.length - 1];

  if (opts.fn) {
    console.log(opts.fn(this));
  } else {
    console.log.apply(console, Array.prototype.slice.call(arguments, 0, -1));
  }
};

/**
 * Trim a string.
 *
 * @return {String}
 */
exports.trim = function () {
  var opts = arguments[arguments.length - 1];

  if (arguments.length > 1) {
    return trim.call(arguments[0] == null ? '' : arguments[0]);
  }

  return trim.call(opts.fn(this));
};

/**
 * Check that a number of arguments are equal.
 *
 * @return {Boolean}
 */
exports.equal = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1);
  var opts = arguments[arguments.length - 1];

  for (var i = 1; i < args.length; i++) {
    if (args[i - 1] !== args[i]) {
      return opts.fn ? opts.inverse(this) : false;
    }
  }

  return opts.fn ? opts.fn(this) : true;
};

/**
 * Serialize as a JSON string.
 *
 * @return {String}
 */
exports.json = function () {
  if (arguments.length > 2) {
    return JSON.stringify(arguments[0], null, arguments[1]);
  }

  if (arguments.length === 2) {
    return JSON.stringify(arguments[0]);
  }

  throw new Error('Unsupported usage of json helper');
};

/**
 * Join an array of values together.
 *
 * @param  {Array}  array
 * @param  {String} value
 * @return {String}
 */
exports.join = function (array, value) {
  return Array.isArray(array) ? array.join(value) : array;
};
