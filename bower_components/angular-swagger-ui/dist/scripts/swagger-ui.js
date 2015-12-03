/*
 * Orange angular-swagger-ui - v0.2.6
 *
 * (C) 2015 Orange, all right reserved
 * MIT Licensed
 */
'use strict';

angular
	.module('swaggerUi', ['ng', 'swaggerUiTemplates'])
	.directive('swaggerUi', ['$injector', function($injector) {

		return {
			restrict: 'A',
			controller: 'swaggerUiController',
			templateUrl: 'templates/swagger-ui.html',
			scope: {
				// Swagger descriptor URL (string, required)
				url: '=',
				// Swagger descriptor parser type (string, optional, default = "auto")
				// Built-in allowed values:
				// 		"auto": (default) parser is based on response Content-Type
				//		"json": force using JSON parser
				//
				//	More types could be defined by external modules
				parser: '@?',
				// Swagger descriptor loading indicator (variables, optional)
				loading: '=?',
				// Use permalinks? (boolean, optional, default = false)
				// If true and if using $routeProvider, should set 'reloadOnSearch: false' in route
				// configuration to avoid UI being rendered multiple times
				permalinks: '=?',
				// Display API explorer (boolean, optional, default = false)
				apiExplorer: '=?',
				// Error handler (function, optional)
				errorHandler: '=?',
				// Are Swagger descriptors loaded from trusted source only ? (boolean, optional, default = false)
				// If true, it avoids using ngSanitize but consider HTML as trusted so won't be cleaned
				trustedSources: '=?',
				// Allows defining a custom Swagger validator or disabling Swagger validation
				// If false, Swagger validation will be disabled
				// If URL, will be used as Swagger validator
				// If not defined, validator will be 'http://online.swagger.io/validator'
				validatorUrl: '@?'
			},
			link: function(scope) {
				// check parameters
				if (scope.permalinks && $injector.has('$route')) {
					var $route = $injector.get('$route');
					if ($route.current && $route.current.$$route && $route.current.$$route.reloadOnSearch) {
						console.warn('AngularSwaggerUI: when using permalinks you should set reloadOnSearch=false in your route config to avoid UI being rebuilt multiple times');
					}
				}
				if (!scope.trustedSources && !$injector.has('$sanitize')) {
					console.warn('AngularSwaggerUI: you must use ngSanitize OR set trusted-sources=true as directive param if swagger descriptors are loaded from trusted sources');
				}
				if (scope.validatorUrl === undefined) {
					scope.validatorUrl = 'http://online.swagger.io/validator';
				}
			}
		};
	}])
	.controller('swaggerUiController', ['$scope', '$http', '$location', '$q', 'swaggerClient', 'swaggerModules', 'swagger2JsonParser',
		function($scope, $http, $location, $q, swaggerClient, swaggerModules, swagger2JsonParser) {

			var swagger;

			// WARNING authentication is not implemented, please use 'api-explorer-transform' directive's param to customize API calls

			// add default Swagger parser (JSON)
			swaggerModules.add(swaggerModules.PARSE, swagger2JsonParser);

			/**
			 * Load Swagger descriptor
			 */
			function loadSwagger(url, callback) {
				$scope.loading = true;
				var options = {
					method: 'GET',
					url: url
				};
				swaggerModules
					.execute(swaggerModules.BEFORE_LOAD, options)
					.then(function() {
						$http(options)
							.success(callback)
							.error(function(data, status) {
								onError({
									code: status,
									message: data
								});
							});
					})
					.catch(onError);
			}

			/**
			 * Swagger descriptor has been loaded, launch parsing
			 */
			function swaggerLoaded(swaggerUrl, swaggerType) {
				$scope.loading = false;
				var parseResult = {};
				// execute modules
				$scope.parser = $scope.parser || 'auto';
				swaggerModules
					.execute(swaggerModules.PARSE, $scope.parser, swaggerUrl, swaggerType, swagger, $scope.trustedSources, parseResult)
					.then(function(executed) {
						if (executed) {
							swaggerParsed(parseResult);
						} else {
							onError({
								code: 415,
								message: 'no parser found for Swagger descriptor of type ' + swaggerType + ' and version ' + swagger.swagger
							});
						}
					})
					.catch(onError);
			}

			/**
			 * Swagger descriptor has parsed, launch display
			 */
			function swaggerParsed(parseResult) {
				// execute modules
				swaggerModules
					.execute(swaggerModules.BEFORE_DISPLAY, parseResult)
					.then(function() {
						// display swagger UI
						$scope.infos = parseResult.infos;
						$scope.form = parseResult.form;
						$scope.resources = parseResult.resources;
					})
					.catch(onError);
			}

			function onError(error) {
				$scope.loading = false;
				if (typeof $scope.errorHandler === 'function') {
					$scope.errorHandler(error.message, error.code);
				} else {
					console.error(error.code, 'AngularSwaggerUI: ' + error.message);
				}
			}

			$scope.$watch('url', function(url) {
				//reset
				$scope.infos = {};
				$scope.resources = [];
				$scope.form = {};
				if (url && url !== '') {
					if ($scope.loading) {
						//TODO cancel current loading swagger
					}
					// load Swagger descriptor
					loadSwagger(url, function(data, status, headers) {
						swagger = data;
						// execute modules
						swaggerModules
							.execute(swaggerModules.BEFORE_PARSE, url, swagger)
							.then(function() {
								var contentType = headers()['content-type'] || 'application/json',
									swaggerType = contentType.split(';')[0];

								swaggerLoaded(url, swaggerType);
							})
							.catch(onError);
					});
				}
			});

			/**
			 * show all resource's operations as list or as expanded list
			 */
			$scope.expand = function(resource, expandOperations) {
				resource.open = true;
				for (var i = 0, op = resource.operations, l = op.length; i < l; i++) {
					op[i].open = expandOperations;
				}
			};

			$scope.permalink = function(name) {
				if ($scope.permalinks) {
					$location.search('swagger', name);
				}
			};

			/**
			 * sends a sample API request
			 */
			$scope.submitExplorer = function(operation) {
				operation.loading = true;
				swaggerClient
					.send(swagger, operation, $scope.form[operation.id])
					.then(function(result) {
						operation.loading = false;
						operation.explorerResult = result;
					});
			};

		}
	])
	.directive('fileInput', function() {
		// helper to be able to retrieve HTML5 File in ngModel from input
		return {
			restrict: 'A',
			require: 'ngModel',
			link: function(scope, element, attr, ngModel) {
				element.bind('change', function() {
					scope.$apply(function() {
						//TODO manage multiple files ?
						ngModel.$setViewValue(element[0].files[0]);
					});
				});
			}
		};
	});
/*
 * Orange angular-swagger-ui - v0.2.6
 *
 * (C) 2015 Orange, all right reserved
 * MIT Licensed
 */
'use strict';

angular
	.module('swaggerUi')
	.service('swaggerClient', ['$q', '$http', 'swaggerModules', function($q, $http, swaggerModules) {

		/**
		 * format API explorer response before display
		 */
		function formatResult(deferred, response) {
			var query = '',
				data = response.data,
				config = response.config;

			if (config.params) {
				var parts = [];
				for (var key in config.params) {
					parts.push(key + '=' + encodeURIComponent(config.params[key]));
				}
				if (parts.length > 0) {
					query = '?' + parts.join('&');
				}
			}
			deferred.resolve({
				url: config.url + query,
				response: {
					body: data ? (angular.isString(data) ? data : angular.toJson(data, true)) : 'no content',
					status: response.status,
					headers: angular.toJson(response.headers(), true)
				}
			});
		}

		/**
		 * Send API explorer request
		 */
		this.send = function(swagger, operation, values) {
			var deferred = $q.defer(),
				query = {},
				headers = {},
				path = operation.path;

			// build request parameters
			for (var i = 0, params = operation.parameters || [], l = params.length; i < l; i++) {
				//TODO manage 'collectionFormat' (csv etc.) !!
				var param = params[i],
					value = values[param.name];

				switch (param.in) {
					case 'query':
						if (!!value) {
							query[param.name] = value;
						}
						break;
					case 'path':
						path = path.replace('{' + param.name + '}', encodeURIComponent(value));
						break;
					case 'header':
						if (!!value) {
							headers[param.name] = value;
						}
						break;
					case 'formData':
						values.body = values.body || new FormData();
						if (!!value) {
							if (param.type === 'file') {
								values.contentType = undefined; // make browser defining it by himself
							}
							values.body.append(param.name, value);
						}
						break;
					case 'body':
						values.body = values.body || value;
						break;
				}
			}

			// add headers
			headers.Accept = values.responseType;
			headers['Content-Type'] = values.body ? values.contentType : 'text/plain';

			// build request
			var baseUrl = [
					swagger.schemes[0],
					'://',
					swagger.host,
					swagger.basePath || ''
				].join(''),
				options = {
					method: operation.httpMethod,
					url: baseUrl + path,
					headers: headers,
					data: values.body,
					params: query
				},
				callback = function(data, status, headers, config) {
					// execute modules
					var response = {
						data: data,
						status: status,
						headers: headers,
						config: config
					};
					swaggerModules
						.execute(swaggerModules.AFTER_EXPLORER_LOAD, response)
						.then(function() {
							formatResult(deferred, response);
						});
				};

			// execute modules
			swaggerModules
				.execute(swaggerModules.BEFORE_EXPLORER_LOAD, options)
				.then(function() {
					// send request
					$http(options)
						.success(callback)
						.error(callback);
				});

			return deferred.promise;
		};

	}]);
/*
 * Orange angular-swagger-ui - v0.2.6
 *
 * (C) 2015 Orange, all right reserved
 * MIT Licensed
 */
'use strict';

angular
	.module('swaggerUi')
	.service('swaggerModel', function() {

		/**
		 * sample object cache to avoid generating the same one multiple times
		 */
		var objCache = {};

		/**
		 * model cache to avoid generating the same one multiple times
		 */
		var modelCache = {};

		/**
		 * retrieves object definition
		 */
		var resolveReference = this.resolveReference = function(swagger, object) {
			if (object.$ref) {
				var parts = object.$ref.replace('#/', '').split('/');
				object = swagger;
				for (var i = 0, j = parts.length; i < j; i++) {
					object = object[parts[i]];
				}
			}
			return object;
		};

		/**
		 * determines a property type
		 */
		var getType = this.getType = function(item) {
			var format = item.format;
			switch (format) {
				case 'int32':
					format = item.type;
					break;
				case 'int64':
					format = 'long';
					break;
			}
			return format || item.type;
		};

		/**
		 * retrieves object class name based on $ref
		 */
		function getClassName(item) {
			var parts = item.$ref.split('/');
			return parts[parts.length - 1];
		}

		/**
		 * generates a sample object (request body or response body)
		 */
		function getSampleObj(swagger, schema, currentGenerated) {
			var sample;
			currentGenerated = currentGenerated || {}; // used to handle circular references
			if (schema.default || schema.example) {
				sample = schema.default || schema.example;
			} else if (schema.properties) {
				sample = {};
				for (var name in schema.properties) {
					sample[name] = getSampleObj(swagger, schema.properties[name], currentGenerated);
				}
			} else if (schema.$ref) {
				// complex object
				var def = resolveReference(swagger, schema);
				if (def) {
					if (!objCache[schema.$ref] && !currentGenerated[schema.$ref]) {
						// object not in cache
						currentGenerated[schema.$ref] = true;
						objCache[schema.$ref] = getSampleObj(swagger, def, currentGenerated);
					}
					sample = objCache[schema.$ref] || {};
				} else {
					console.warn('schema not found', schema.$ref);
				}
			} else if (schema.type === 'array') {
				sample = [getSampleObj(swagger, schema.items, currentGenerated)];
			} else if (schema.type === 'object') {
				sample = {};
			} else {
				sample = getSampleValue(getType(schema));
				sample = schema.defaultValue || schema.example || getSampleValue(getType(schema));
			}
			return sample;
		}

		/**
		 * generates a sample value for a basic type
		 */
		function getSampleValue(type) {
			var result;
			switch (type) {
				case 'long':
				case 'integer':
					result = 0;
					break;
				case 'boolean':
					result = false;
					break;
				case 'double':
				case 'number':
					result = 0.0;
					break;
				case 'string':
					result = 'string';
					break;
				case 'date':
					result = (new Date()).toISOString().split('T')[0];
					break;
				case 'date-time':
					result = (new Date()).toISOString();
					break;
			}
			return result;
		}

		/**
		 * generates a sample JSON string (request body or response body)
		 */
		this.generateSampleJson = function(swagger, schema) {
			var json,
				obj = getSampleObj(swagger, schema);

			if (obj) {
				json = angular.toJson(obj, true);
			}
			return json;
		};

		/**
		 * inline model counter
		 */
		var countInLine = 0;

		/**
		 * generates object's model
		 */
		var generateModel = this.generateModel = function(swagger, schema, modelName, currentGenerated) {
			var model = '';
			currentGenerated = currentGenerated || {}; // used to handle circular references

			function isRequired(item, name) {
				return item.required && item.required.indexOf(name) !== -1;
			}

			if (schema.properties) {
				modelName = modelName || ('Inline Model' + countInLine++);
				currentGenerated[modelName] = true;
				var buffer = ['<div><strong>' + modelName + ' {</strong>'],
					submodels = [];

				for (var propertyName in schema.properties) {
					var property = schema.properties[propertyName];
					buffer.push('<div class="pad"><strong>', propertyName, '</strong> (<span class="type">');
					// build type
					if (property.properties) {
						var name = 'Inline Model' + countInLine++;
						buffer.push(name);
						submodels.push(generateModel(swagger, property, name, currentGenerated));
					} else if (property.$ref) {
						buffer.push(getClassName(property));
						submodels.push(generateModel(swagger, property, null, currentGenerated));
					} else if (property.type === 'array') {
						buffer.push('Array[');
						if (property.items.properties) {
							var name = 'Inline Model' + countInLine++;
							buffer.push(name);
							submodels.push(generateModel(swagger, property, name, currentGenerated));
						} else if (property.items.$ref) {
							buffer.push(getClassName(property.items));
							submodels.push(generateModel(swagger, property.items, null, currentGenerated));
						} else {
							buffer.push(getType(property.items));
						}
						buffer.push(']');
					} else {
						buffer.push(getType(property));
					}
					buffer.push('</span>');
					// is required ?
					if (!isRequired(schema, propertyName)) {
						buffer.push(', ', '<em>optional</em>');
					}
					buffer.push(')');
					// has description
					if (property.description) {
						buffer.push(': ', property.description);
					}
					// is enum
					if (property.enum) {
						buffer.push(' = ', angular.toJson(property.enum).replace(/,/g, ' or '));
					}
					buffer.push(',</div>');
				}
				buffer.pop();
				buffer.push('</div>');
				buffer.push('<strong>}</strong>');
				buffer.push(submodels.join(''), '</div>');
				model = buffer.join('');
			} else if (schema.$ref) {
				var className = getClassName(schema),
					def = resolveReference(swagger, schema);

				if (currentGenerated[className]) {
					return ''; // already generated
				}
				if (def) {
					if (!modelCache[schema.$ref]) {
						// cache generated object
						modelCache[schema.$ref] = generateModel(swagger, def, className, currentGenerated);
					}
					currentGenerated[className] = true;
					model = modelCache[schema.$ref];
				}
			} else if (schema.type === 'array') {
				var buffer = ['<strong>Array ['];
				var sub = '';
				if (schema.items.properties) {
					var name = 'Inline Model' + countInLine++;
					buffer.push(name);
					sub = generateModel(swagger, schema.items, name, currentGenerated);
				} else if (schema.items.$ref) {
					buffer.push(getClassName(schema.items));
					sub = generateModel(swagger, schema.items, null, currentGenerated);
				} else {
					buffer.push(getType(schema.items));
				}
				buffer.push(']</strong><br><br>', sub);
				model = buffer.join('');
			} else if (schema.type === 'object') {
				model = '<strong>Inline Model {<br>}</strong>';
			}
			return model;
		};

		/**
		 * clears generated models cache
		 */
		this.clearCache = function() {
			objCache = {};
			modelCache = {};
		};

	});
/*
 * Orange angular-swagger-ui - v0.2.6
 *
 * (C) 2015 Orange, all right reserved
 * MIT Licensed
 */
'use strict';

angular
	.module('swaggerUi')
	.service('swaggerModules', ['$q', function($q) {

		var modules = {};

		this.BEFORE_LOAD = 'BEFORE_LOAD';
		this.BEFORE_PARSE = 'BEFORE_PARSE';
		this.PARSE = 'PARSE';
		this.BEFORE_DISPLAY = 'BEFORE_DISPLAY';
		this.BEFORE_EXPLORER_LOAD = 'BEFORE_EXPLORER_LOAD';
		this.AFTER_EXPLORER_LOAD = 'AFTER_EXPLORER_LOAD';

		/**
		 * Adds a new module to swagger-ui
		 */
		this.add = function(phase, module) {
			if (!modules[phase]) {
				modules[phase] = [];
			}
			if (modules[phase].indexOf(module) < 0) {
				modules[phase].push(module);
			}
		};

		/**
		 * Runs modules' "execute" function one by one
		 */
		function executeAll(deferred, phaseModules, args, phaseExecuted) {
			var module = phaseModules.shift();
			if (module) {
				module
					.execute.apply(module, args)
					.then(function(executed) {
						phaseExecuted = phaseExecuted || executed;
						executeAll(deferred, phaseModules, args, phaseExecuted);
					})
					.catch(deferred.reject);
			} else {
				deferred.resolve(phaseExecuted);
			}
		}

		/**
		 * Executes modules' phase
		 */
		this.execute = function() {
			var args = Array.prototype.slice.call(arguments), // get an Array from arguments
				phase = args.splice(0, 1),
				deferred = $q.defer(),
				phaseModules = modules[phase] || [];

			executeAll(deferred, [].concat(phaseModules), args);
			return deferred.promise;
		};

	}]);
/*
 * Orange angular-swagger-ui - v0.2.6
 *
 * (C) 2015 Orange, all right reserved
 * MIT Licensed
 */
'use strict';

angular
	.module('swaggerUi')
	.service('swagger2JsonParser', ['$q', '$sce', '$location', 'swaggerModel', function($q, $sce, $location, swaggerModel) {

		var trustedSources;

		/**
		 * parse swagger description to ease HTML generation
		 */
		function parseSwagger2Json(swagger, url, deferred, parseResult) {
			var map = {},
				form = {},
				resources = [],
				infos = swagger.info,
				openPath = $location.search().swagger,
				defaultContentType = 'application/json';

			parseInfos(swagger, url, infos, defaultContentType);
			parseTags(swagger, resources, map);
			parseOperations(swagger, resources, form, map, defaultContentType, openPath);
			cleanUp(resources, openPath);
			// prepare result
			parseResult.infos = infos;
			parseResult.resources = resources;
			parseResult.form = form;
			deferred.resolve(true);
		}

		/**
		 * parse main infos
		 */
		function parseInfos(swagger, url, infos, defaultContentType) {
			// build URL params
			var a = angular.element('<a href="' + url + '"></a>')[0];
			swagger.schemes = [swagger.schemes && swagger.schemes[0] || a.protocol.replace(':', '')];
			swagger.host = swagger.host || a.host;
			swagger.consumes = swagger.consumes || [defaultContentType];
			swagger.produces = swagger.produces || [defaultContentType];
			// build main infos
			infos.scheme = swagger.schemes[0];
			infos.basePath = swagger.basePath;
			infos.host = swagger.host;
			infos.description = trustHtml(infos.description);
		}

		/**
		 * parse tags
		 */
		function parseTags(swagger, resources, map) {
			var i, l, tag;
			if (!swagger.tags) {
				resources.push({
					name: 'default',
					open: true
				});
				map['default'] = 0;
			} else {
				for (i = 0, l = swagger.tags.length; i < l; i++) {
					tag = swagger.tags[i];
					resources.push(tag);
					map[tag.name] = i;
				}
			}
		}

		/**
		 * parse operations
		 */
		function parseOperations(swagger, resources, form, map, defaultContentType, openPath) {
			var path,
				pathObject,
				pathParameters,
				httpMethod,
				operation,
				tag,
				resource,
				operationId = 0,
				paramId = 0;

			for (path in swagger.paths) {
				pathObject = swagger.paths[path];
				pathParameters = pathObject.parameters || [];
				delete pathObject.parameters;
				for (httpMethod in pathObject) {
					operation = pathObject[httpMethod];
					//TODO manage 'deprecated' operations ?
					operation.id = operationId;
					operation.description = trustHtml(operation.description);
					operation.produces = operation.produces || swagger.produces;
					form[operationId] = {
						responseType: defaultContentType
					};
					operation.httpMethod = httpMethod;
					operation.path = path;
					parseParameters(swagger, operation, pathParameters, form, defaultContentType, operationId, paramId);
					parseResponses(swagger, operation);
					operation.tags = operation.tags || ['default'];
					// map operation to resource
					tag = operation.tags[0];
					if (typeof map[tag] === 'undefined') {
						map[tag] = resources.length;
						resources.push({
							name: tag
						});
					}
					resource = resources[map[operation.tags[0]]];
					operation.open = openPath && openPath === operation.operationId || openPath === resource.name + '*';
					resource.operations = resource.operations || [];
					resource.operations.push(operation);
					if (operation.open) {
						resource.open = true;
					}
					operationId++;
				}
			}
		}

		/**
		 * compute path and operation parameters
		 */
		function computeParameters(swagger, pathParameters, operation) {
			var i, j, k, l,
				operationParameters = operation.parameters || [],
				parameters = [].concat(operationParameters),
				found,
				pathParameter,
				operationParameter;

			for (i = 0, l = pathParameters.length; i < l; i++) {
				found = false;
				pathParameter = swaggerModel.resolveReference(swagger, pathParameters[i]);

				for (j = 0, k = operationParameters.length; j < k; j++) {
					operationParameter = swaggerModel.resolveReference(swagger, operationParameters[j]);
					if (pathParameter.name === operationParameter.name && pathParameter.in === operationParameter.in) {
						// overriden parameter
						found = true;
						break;
					}
				}
				if (!found) {
					// add path parameter to operation ones
					parameters.push(pathParameter);
				}
			}
			return parameters;
		}

		/**
		 * parse operation parameters
		 */
		function parseParameters(swagger, operation, pathParameters, form, defaultContentType, operationId, paramId) {
			var i, l,
				param,
				parameters = operation.parameters = computeParameters(swagger, pathParameters, operation);

			for (i = 0, l = parameters.length; i < l; i++) {
				//TODO manage 'collectionFormat' (csv, multi etc.) ?
				//TODO manage constraints (pattern, min, max etc.) ?
				param = parameters[i] = swaggerModel.resolveReference(swagger, parameters[i]);
				param.id = paramId;
				param.type = swaggerModel.getType(param);
				param.description = trustHtml(param.description);
				if (param.items && param.items.enum) {
					param.enum = param.items.enum;
					param.default = param.items.default;
				}
				param.subtype = param.enum ? 'enum' : param.type;
				// put param into form scope
				form[operationId][param.name] = param.default || '';
				if (param.schema) {
					param.schema.display = 1; // display schema
					param.schema.json = swaggerModel.generateSampleJson(swagger, param.schema);
					param.schema.model = $sce.trustAsHtml(swaggerModel.generateModel(swagger, param.schema));
				}
				if (param.in === 'body') {
					operation.consumes = operation.consumes || swagger.consumes;
					form[operationId].contentType = operation.consumes.length === 1 ? operation.consumes[0] : defaultContentType;
				}
				paramId++;
			}
		}

		/**
		 * parse operatiopn responses
		 */
		function parseResponses(swagger, operation) {
			var code,
				response;

			if (operation.responses) {
				for (code in operation.responses) {
					//TODO manage response headers
					response = operation.responses[code];
					response.description = trustHtml(response.description);
					if (response.schema) {
						response.schema.json = response.examples && response.examples[operation.produces[0]] || swaggerModel.generateSampleJson(swagger, response.schema);
						if (response.schema.type === 'object' || response.schema.type === 'array' || response.schema.$ref) {
							response.display = 1; // display schema
							response.schema.model = $sce.trustAsHtml(swaggerModel.generateModel(swagger, response.schema));
						} else if (response.schema.type === 'string') {
							delete response.schema;
						}
						if (code === '200' || code === '201') {
							operation.responseClass = response;
							operation.responseClass.display = 1;
							operation.responseClass.status = code;
							delete operation.responses[code];
						} else {
							operation.hasResponses = true;
						}
					} else {
						operation.hasResponses = true;
					}
				}
			}
		}

		function cleanUp(resources, openPath) {
			var i,
				resource,
				operations;

			for (i = 0; i < resources.length; i++) {
				resource = resources[i];
				operations = resources[i].operations;
				resource.open = resource.open || openPath === resource.name || openPath === resource.name + '*';
				if (!operations || (operations && operations.length === 0)) {
					resources.splice(i--, 1);
				}
			}
			// sort resources alphabeticaly
			resources.sort(function(a, b) {
				if (a.name > b.name) {
					return 1;
				} else if (a.name < b.name) {
					return -1;
				}
				return 0;
			});
			swaggerModel.clearCache();
		}

		function trustHtml(text) {
			var trusted = text;
			if (typeof text === 'string' && trustedSources) {
				trusted = $sce.trustAsHtml(escapeChars(text));
			}
			// else ngSanitize MUST be added to app
			return trusted;
		}

		function escapeChars(text) {
			return text && text
				.replace(/&/g, '&amp;')
				.replace(/<([^\/a-zA-Z])/g, '&lt;$1')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#039;');
		}

		/**
		 * Module entry point
		 */
		this.execute = function(parserType, url, contentType, data, isTrustedSources, parseResult) {
			var deferred = $q.defer();
			if (data.swagger === '2.0' && (parserType === 'json' || (parserType === 'auto' && contentType === 'application/json'))) {
				trustedSources = isTrustedSources;
				try {
					parseSwagger2Json(data, url, deferred, parseResult);
				} catch (e) {
					deferred.reject({
						code: 500,
						message: 'failed to parse swagger: ' + e.message
					});
				}
			} else {
				deferred.resolve(false);
			}
			return deferred.promise;
		};

	}]);

angular.module('swaggerUiTemplates', ['templates/endpoint.html', 'templates/operation.html', 'templates/parameter.html', 'templates/response.html', 'templates/swagger-ui.html']);

angular.module('templates/endpoint.html', []).run(['$templateCache', function($templateCache) {
  $templateCache.put('templates/endpoint.html',
    '<div class="clearfix"> <ul class="list-inline pull-left endpoint-heading"> <li> <h4> <a href="javascript:;" ng-click="api.open=!api.open;permalink(api.open?api.name:null)" ng-bind="api.name"></a> <span ng-if="api.description"> : <span ng-bind="api.description"></span></span> </h4> </li> </ul> <ul class="list-inline pull-right endpoint-actions"> <li> <a href="javascript:;" ng-click="api.open=!api.open;permalink(api.open?api.name:null)">open/hide</a> </li> <li> <a href="javascript:;" ng-click="expand(api);permalink(api.name)">list operations</a> </li> <li> <a href="javascript:;" ng-click="expand(api,true);permalink(api.name+\'*\')">expand operations</a> </li> </ul> </div> <ul class="list-unstyled collapse operations" ng-class="{in:api.open}"> <li ng-repeat="op in api.operations track by $index" class="operation {{op.httpMethod}}" ng-include="\'templates/operation.html\'"></li> </ul>');
}]);

angular.module('templates/operation.html', []).run(['$templateCache', function($templateCache) {
  $templateCache.put('templates/operation.html',
    '<div class="heading"> <a ng-click="op.open=!op.open;permalink(op.open?op.operationId:null)" href="javascript:;"> <div class="clearfix"> <span class="http-method text-uppercase" ng-bind="op.httpMethod"></span> <span class="path" ng-bind="op.path"></span> <span class="description pull-right" ng-bind="op.summary"></span> </div> </a> </div> <div class="content collapse" ng-class="{in:op.open}"> <div ng-if="op.description"> <h5>implementation notes</h5> <p ng-bind-html="op.description"></p> </div> <form role="form" name="explorerForm" ng-submit="explorerForm.$valid&&submitExplorer(op)"> <div ng-if="op.responseClass" class="response"> <h5>response class (status {{op.responseClass.status}})</h5> <div ng-if="op.responseClass.display!==-1"> <ul class="list-inline schema"> <li><a href="javascript:;" ng-click="op.responseClass.display=0" ng-class="{active:op.responseClass.display===0}">model</a></li> <li><a href="javascript:;" ng-click="op.responseClass.display=1" ng-class="{active:op.responseClass.display===1}">model schema</a></li> </ul> <pre class="model" ng-if="op.responseClass.display===0" ng-bind-html="op.responseClass.schema.model"></pre> <pre class="model-schema" ng-if="op.responseClass.display===1" ng-bind="op.responseClass.schema.json"></pre> </div> <div ng-if="op.produces" class="content-type"> <label for="responseContentType{{op.id}}">response content type</label> <select ng-model="form[op.id].responseType" ng-options="item for item in op.produces track by item" id="responseContentType{{op.id}}" name="responseContentType{{op.id}}" required></select> </div> </div> <div ng-if="op.parameters&&op.parameters.length>0" class="table-responsive"> <h5>parameters</h5> <table class="table table-condensed parameters"> <thead> <tr> <th class="name">parameter <th class="value">value <th class="desc">description <th class="type">parameter type <th class="data">data type   <tbody> <tr ng-repeat="param in op.parameters track by $index" ng-include="\'templates/parameter.html\'">  </table> </div> <div class="table-responsive" ng-if="op.hasResponses"> <h5>response messages</h5> <table class="table responses"> <thead> <tr> <th class="code">HTTP status code <th>reason <th>response model   <tbody> <tr ng-repeat="(code, resp) in op.responses track by $index" ng-include="\'templates/response.html\'">  </table> </div> <div ng-if="apiExplorer"> <button class="btn btn-default" ng-click="op.explorerResult=false;op.hideExplorerResult=false" type="submit" ng-disabled="op.loading" ng-bind="op.loading?\'loading...\':\'try it out!\'"></button> <a class="hide-try-it" ng-if="op.explorerResult&&!op.hideExplorerResult" ng-click="op.hideExplorerResult=true" href="javascript:;">hide response</a> </div> </form> <div ng-if="op.explorerResult" ng-show="!op.hideExplorerResult"> <h5>request URL</h5> <pre ng-bind="op.explorerResult.url"></pre> <h5>response body</h5> <pre ng-bind="op.explorerResult.response.body"></pre> <h5>response code</h5> <pre ng-bind="op.explorerResult.response.status"></pre> <h5>response headers</h5> <pre ng-bind="op.explorerResult.response.headers"></pre> </div> </div>');
}]);

angular.module('templates/parameter.html', []).run(['$templateCache', function($templateCache) {
  $templateCache.put('templates/parameter.html',
    '<td ng-class="{bold:param.required}"> <label for="param{{param.id}}" ng-bind="param.name"></label>  <td ng-class="{bold:param.required}"> <div ng-if="apiExplorer"> <div ng-if="param.in!==\'body\'" ng-switch="param.subtype"> <input ng-switch-when="file" type="file" file-input ng-model="form[op.id][param.name]" id="param{{param.id}}" placeholder="{{param.required?\'(required)\':\'\'}}" ng-required="param.required"> <select ng-switch-when="enum" ng-model="form[op.id][param.name]" id="param{{param.id}}"> <option ng-repeat="value in param.enum" value="{{value}}" ng-bind="value+(param.default===value?\' (default)\':\'\')" ng-selected="param.default===value"> </select> <input ng-switch-default type="text" ng-model="form[op.id][param.name]" id="param{{param.id}}" placeholder="{{param.required?\'(required)\':\'\'}}" ng-required="param.required"> </div> <div ng-if="param.in===\'body\'"> <textarea id="param{{param.id}}" ng-model="form[op.id][param.name]" ng-required="param.required"></textarea> <br> <div ng-if="op.consumes" class="content-type"> <label for="bodyContentType{{op.id}}">parameter content type</label> <select ng-model="form[op.id].contentType" id="bodyContentType{{op.id}}" name="bodyContentType{{op.id}}" ng-options="item for item in op.consumes track by item"></select> </div> </div> </div> <div ng-if="!apiExplorer"> <div ng-if="param.in!==\'body\'"> <div ng-if="param.default"><span ng-bind="param.default"></span> (default)</div> <div ng-if="param.enum"> <span ng-repeat="value in param.enum track by $index">{{value}}<span ng-if="!$last"> or </span></span> </div> <div ng-if="param.required"><strong>(required)</strong></div> </div> </div>  <td ng-class="{bold:param.required}" ng-bind-html="param.description"> <td ng-bind="param.in"> <td ng-if="param.type" ng-switch="param.type"> <span ng-switch-when="array" ng-bind="\'Array[\'+param.items.type+\']\'"></span> <span ng-switch-default ng-bind="param.type"></span>  <td ng-if="param.schema"> <ul class="list-inline schema"> <li><a href="javascript:;" ng-click="param.schema.display=0" ng-class="{active:param.schema.display===0}">model</a></li> <li><a href="javascript:;" ng-click="param.schema.display=1" ng-class="{active:param.schema.display===1}">model schema</a></li> </ul> <pre class="model" ng-if="param.schema.display===0&&param.schema.model" ng-bind-html="param.schema.model"></pre> <div class="model-schema" ng-if="param.schema.display===1&&param.schema.json"> <pre ng-bind="param.schema.json" ng-click="form[op.id][param.name]=param.schema.json" aria-described-by="help-{{param.id}}"></pre> <div id="help-{{param.id}}">click to set as parameter value</div> </div> ');
}]);

angular.module('templates/response.html', []).run(['$templateCache', function($templateCache) {
  $templateCache.put('templates/response.html',
    '<td ng-bind="code"> <td ng-bind-html="resp.description"> <td> <ul ng-if="resp.schema&&resp.schema.model&&resp.schema.json" class="list-inline schema"> <li><a href="javascript:;" ng-click="resp.display=0" ng-class="{active:resp.display===0}">model</a></li> <li><a href="javascript:;" ng-click="resp.display=1" ng-class="{active:resp.display===1}">model schema</a></li> </ul> <pre class="model" ng-if="resp.display===0&&resp.schema&&resp.schema.model" ng-bind-html="resp.schema.model"></pre> <pre class="model-schema" ng-if="resp.display===1&&resp.schema&&resp.schema.json" ng-bind="resp.schema.json"></pre> ');
}]);

angular.module('templates/swagger-ui.html', []).run(['$templateCache', function($templateCache) {
  $templateCache.put('templates/swagger-ui.html',
    '<div class="swagger-ui" aria-live="polite" aria-relevant="additions removals"> <div class="api-name"> <h3 ng-bind="infos.title"></h3> </div> <div class="api-description" ng-bind-html="infos.description"></div> <div class="api-infos"> <div class="api-infos-contact" ng-if="infos.contact"> <div ng-if="infos.contact.name" class="api-infos-contact-name">created by <span ng-bind="infos.contact.name"></span></div> <div ng-if="infos.contact.url" class="api-infos-contact-url">see more at <a href="{{infos.contact.url}}" ng-bind="infos.contact.url"></a></div> <a ng-if="infos.contact.email" class="api-infos-contact-url" href="mailto:{{infos.contact.email}}?subject={{infos.title}}">contact the developer</a> </div> <div class="api-infos-license" ng-if="infos.license"> <span>license: </span><a href="{{infos.license.url}}" ng-bind="infos.license.name"></a> </div> </div> <ul class="list-unstyled endpoints"> <li ng-repeat="api in resources track by $index" class="endpoint" ng-class="{active:api.open}" ng-include="\'templates/endpoint.html\'"></li> </ul> <div class="api-version clearfix" ng-if="infos"> [BASE URL: <span class="h4" ng-bind="infos.basePath"></span>, API VERSION: <span class="h4" ng-bind="infos.version"></span>, HOST: <span class="h4" ng-bind="infos.scheme"></span>://<span class="h4" ng-bind="infos.host"></span>] <a ng-if="validatorUrl!=\'false\'" target="_blank" href="{{validatorUrl}}/debug?url={{url}}"><img class="pull-right swagger-validator" ng-src="{{validatorUrl}}?url={{url}}"></a> </div> </div>');
}]);
