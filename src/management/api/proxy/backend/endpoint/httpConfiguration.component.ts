/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as _ from 'lodash';

const HttpConfigurationComponent: ng.IComponentOptions = {
  bindings: {
    httpConfiguration: '=',
    inheritHttpConfiguration: '<',
    form: '<'
  },
  controller: class {
    private httpConfiguration: any;
    private inheritHttpConfiguration: any;
    private form: any;
    private proxies: any[];
    private trustStoreTypes: any[];
    private keyStoreTypes: any[];

    $onInit() {
      this.proxies = [
        {
          name: 'HTTP CONNECT proxy',
          value: 'HTTP'
        }, {
          name: 'SOCKS4/4a tcp proxy',
          value: 'SOCKS4'
        }, {
          name: 'SOCKS5 tcp proxy',
          value: 'SOCKS5'
        }];

      this.trustStoreTypes = [
        {
          name: 'None',
          value: ''
        }, {
          name: 'Java Trust Store (.jks)',
          value: 'JKS'
        }, {
          name: 'PKCS#12 (.p12) / PFX (.pfx)',
          value: 'PKCS12'
        }, {
          name: 'PEM (.pem)',
          value: 'PEM'
        }];

      this.keyStoreTypes = [
        {
          name: 'None',
          value: ''
        },
        {
          name: 'Java Trust Store (.jks)',
          value: 'JKS'
        }, {
          name: 'PKCS#12 (.p12) / PFX (.pfx)',
          value: 'PKCS12'
        }, {
          name: 'PEM (.pem)',
          value: 'PEM'
        }];

      this.initModel();
    };

    toggleInherit(inherit) {
      if (inherit) {
        delete this.httpConfiguration.http;
        delete this.httpConfiguration.ssl;
        delete this.httpConfiguration.proxy;
        delete this.httpConfiguration.headers;
      } else {
        this.httpConfiguration.http = _.cloneDeep(this.inheritHttpConfiguration.http);
        this.httpConfiguration.ssl = _.cloneDeep(this.inheritHttpConfiguration.ssl);
        this.httpConfiguration.proxy = _.cloneDeep(this.inheritHttpConfiguration.proxy);
        this.httpConfiguration.headers = _.cloneDeep(this.inheritHttpConfiguration.headers);
        this.initModel();
      }
    }

    initModel() {
      this.httpConfiguration.ssl = this.httpConfiguration.ssl || {trustAll: false};
      this.httpConfiguration.ssl.trustStore = this.httpConfiguration.ssl.trustStore || {type: ''};
      this.httpConfiguration.ssl.keyStore = this.httpConfiguration.ssl.keyStore || {type: ''};

      this.httpConfiguration.headers = (this.httpConfiguration.headers) ?
        Object
          .keys(this.httpConfiguration.headers)
          .map(name => ({name, value: this.httpConfiguration.headers[name]})) : [];

      if (!this.httpConfiguration.http) {
        this.httpConfiguration.http = {
          connectTimeout: 5000,
          idleTimeout: 60000,
          keepAlive: true,
          readTimeout: 10000,
          pipelining: false,
          maxConcurrentConnections: 100,
          useCompression: true,
          followRedirects: false
        };
      }
      if (!this.httpConfiguration.ssl) {
        this.httpConfiguration.ssl = {
          trustAll: false,
          trustStore: {
            type: ''
          }
        };
      }
      if (!this.httpConfiguration.proxy) {
        this.httpConfiguration.proxy = {};
      }
      if (!this.httpConfiguration.headers) {
        this.httpConfiguration.headers = {};
      }
    }

    addHTTPHeader() {
      this.httpConfiguration.headers.push({name: '', value: ''});
    }

    removeHTTPHeader(idx) {
      if (this.httpConfiguration.headers !== undefined) {
        this.httpConfiguration.headers.splice(idx, 1);
        this.form.$setDirty();
      }
    }
  },
  template: require('./httpConfiguration.html')
};

export default HttpConfigurationComponent;

