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
import { Grammar } from './grammar';

export function fakeGrammar(attributes?: Partial<Grammar>): Grammar {
  return {
    ...base,
    ...attributes,
  };
}

const base: Grammar = {
  request: {
    content: {
      _type: 'String',
    },
    contextPath: {
      _type: 'String',
    },
    headers: {
      _type: 'HttpHeaders',
    },
    id: {
      _type: 'String',
    },
    localAddress: {
      _type: 'String',
    },
    params: {
      _type: 'MultiValueMap',
    },
    path: {
      _type: 'String',
    },
    pathInfo: {
      _type: 'String',
    },
    pathInfos: {
      _type: 'String[]',
    },
    pathParams: {
      _type: 'MultiValueMap',
    },
    paths: {
      _type: 'String[]',
    },
    remoteAddress: {
      _type: 'String',
    },
    scheme: {
      _type: 'String',
    },
    ssl: {
      client: {
        attributes: {
          _type: 'MultiValueMap',
        },
        businessCategory: {
          _type: 'String',
        },
        c: {
          _type: 'String',
        },
        cn: {
          _type: 'String',
        },
        countryOfCitizenship: {
          _type: 'String',
        },
        countryOfResidence: {
          _type: 'String',
        },
        dateOfBirth: {
          _type: 'String',
        },
        dc: {
          _type: 'String',
        },
        defined: {
          _type: 'boolean',
        },
        description: {
          _type: 'String',
        },
        dmdName: {
          _type: 'String',
        },
        dn: {
          _type: 'String',
        },
        dnQualifier: {
          _type: 'String',
        },
        e: {
          _type: 'String',
        },
        emailAddress: {
          _type: 'String',
        },
        gender: {
          _type: 'String',
        },
        generation: {
          _type: 'String',
        },
        givenname: {
          _type: 'String',
        },
        initials: {
          _type: 'String',
        },
        l: {
          _type: 'String',
        },
        name: {
          _type: 'String',
        },
        nameAtBirth: {
          _type: 'String',
        },
        o: {
          _type: 'String',
        },
        organizationIdentifier: {
          _type: 'String',
        },
        ou: {
          _type: 'String',
        },
        placeOfBirth: {
          _type: 'String',
        },
        postalAddress: {
          _type: 'String',
        },
        postalCode: {
          _type: 'String',
        },
        pseudonym: {
          _type: 'String',
        },
        role: {
          _type: 'String',
        },
        serialnumber: {
          _type: 'String',
        },
        st: {
          _type: 'String',
        },
        street: {
          _type: 'String',
        },
        surname: {
          _type: 'String',
        },
        t: {
          _type: 'String',
        },
        telephoneNumber: {
          _type: 'String',
        },
        uid: {
          _type: 'String',
        },
        uniqueIdentifier: {
          _type: 'String',
        },
        unstructuredAddress: {
          _type: 'String',
        },
      },
      clientHost: {
        _type: 'String',
      },
      clientPort: {
        _type: 'Integer',
      },
      server: {
        attributes: {
          _type: 'MultiValueMap',
        },
        businessCategory: {
          _type: 'String',
        },
        c: {
          _type: 'String',
        },
        cn: {
          _type: 'String',
        },
        countryOfCitizenship: {
          _type: 'String',
        },
        countryOfResidence: {
          _type: 'String',
        },
        dateOfBirth: {
          _type: 'String',
        },
        dc: {
          _type: 'String',
        },
        defined: {
          _type: 'boolean',
        },
        description: {
          _type: 'String',
        },
        dmdName: {
          _type: 'String',
        },
        dn: {
          _type: 'String',
        },
        dnQualifier: {
          _type: 'String',
        },
        e: {
          _type: 'String',
        },
        emailAddress: {
          _type: 'String',
        },
        gender: {
          _type: 'String',
        },
        generation: {
          _type: 'String',
        },
        givenname: {
          _type: 'String',
        },
        initials: {
          _type: 'String',
        },
        l: {
          _type: 'String',
        },
        name: {
          _type: 'String',
        },
        nameAtBirth: {
          _type: 'String',
        },
        o: {
          _type: 'String',
        },
        organizationIdentifier: {
          _type: 'String',
        },
        ou: {
          _type: 'String',
        },
        placeOfBirth: {
          _type: 'String',
        },
        postalAddress: {
          _type: 'String',
        },
        postalCode: {
          _type: 'String',
        },
        pseudonym: {
          _type: 'String',
        },
        role: {
          _type: 'String',
        },
        serialnumber: {
          _type: 'String',
        },
        st: {
          _type: 'String',
        },
        street: {
          _type: 'String',
        },
        surname: {
          _type: 'String',
        },
        t: {
          _type: 'String',
        },
        telephoneNumber: {
          _type: 'String',
        },
        uid: {
          _type: 'String',
        },
        uniqueIdentifier: {
          _type: 'String',
        },
        unstructuredAddress: {
          _type: 'String',
        },
      },
    },
    timestamp: {
      _type: 'long',
    },
    transactionId: {
      _type: 'String',
    },
    uri: {
      _type: 'String',
    },
    version: {
      _type: 'String',
    },
  },
  endpoints: {
    _type: 'String[]',
  },
  response: {
    headers: {
      _type: 'HttpHeaders',
    },
    content: {
      _type: 'String',
    },
    status: {
      _type: 'int',
    },
  },
  context: {
    attributes: {
      'context-path': {
        _type: 'String',
      },
      application: {
        _type: 'String',
      },
      'api-key': {
        _type: 'String',
      },
      'user-id': {
        _type: 'String',
      },
      api: {
        _type: 'String',
      },
      plan: {
        _type: 'String',
      },
      'resolved-path': {
        _type: 'String',
      },
    },
  },
  _enums: {
    HttpHeaders: [
      'Accept',
      'Accept-Charset',
      'Accept-Encoding',
      'Accept-Language',
      'Accept-Ranges',
      'Access-Control-Allow-Credentials',
      'Access-Control-Allow-Headers',
      'Access-Control-Allow-Methods',
      'Access-Control-Allow-Origin',
      'Access-Control-Expose-Headers',
      'Access-Control-Max-Age',
      'Access-Control-Request-Headers',
      'Access-Control-Request-Method',
      'Age',
      'Allow',
      'Authorization',
      'Cache-Control',
      'Connection',
      'Content-Disposition',
      'Content-Encoding',
      'Content-ID',
      'Content-Language',
      'Content-Length',
      'Content-Location',
      'Content-MD5',
      'Content-Range',
      'Content-Type',
      'Cookie',
      'Date',
      'ETag',
      'Expires',
      'Expect',
      'Forwarded',
      'From',
      'Host',
      'If-Match',
      'If-Modified-Since',
      'If-None-Match',
      'If-Unmodified-Since',
      'Keep-Alive',
      'Last-Modified',
      'Location',
      'Link',
      'Max-Forwards',
      'MIME-Version',
      'Origin',
      'Pragma',
      'Proxy-Authenticate',
      'Proxy-Authorization',
      'Proxy-Connection',
      'Range',
      'Referer',
      'Retry-After',
      'Server',
      'Set-Cookie',
      'Set-Cookie2',
      'TE',
      'Trailer',
      'Transfer-Encoding',
      'Upgrade',
      'User-Agent',
      'Vary',
      'Via',
      'Warning',
      'WWW-Authenticate',
      'X-Forwarded-For',
      'X-Forwarded-Proto',
      'X-Forwarded-Server',
      'X-Forwarded-Host',
      'X-Forwarded-Port',
      'X-Forwarded-Prefix',
    ],
  },
  properties: {
    _type: 'String[]',
  },
  dictionaries: {
    _type: 'String[]',
  },
  _types: {
    MultiValueMap: {
      methods: [
        {
          name: 'containsKey',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'containsValue',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'entrySet',
          returnType: 'Set',
        },
        {
          name: 'get',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'getOrDefault',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
            {
              name: 'arg1',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'keySet',
          returnType: 'Set',
        },
        {
          name: 'size',
          returnType: 'int',
        },
        {
          name: 'values',
          returnType: 'Collection',
        },
      ],
    },
    HttpHeaders: {
      methods: [
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
        {
          name: 'containsKey',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'containsValue',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'entrySet',
          returnType: 'Set',
        },
        {
          name: 'get',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'getOrDefault',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
            {
              name: 'arg1',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'keySet',
          returnType: 'Set',
        },
        {
          name: 'size',
          returnType: 'int',
        },
        {
          name: 'values',
          returnType: 'Collection',
        },
      ],
    },
    Map: {
      methods: [
        {
          name: 'containsKey',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'containsValue',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'entrySet',
          returnType: 'Set',
        },
        {
          name: 'get',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'getOrDefault',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
            {
              name: 'arg1',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'keySet',
          returnType: 'Set',
        },
        {
          name: 'size',
          returnType: 'int',
        },
        {
          name: 'values',
          returnType: 'Collection',
        },
      ],
    },
    Boolean: {
      methods: [
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    Integer: {
      methods: [
        {
          name: 'numberOfLeadingZeros',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'numberOfTrailingZeros',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'bitCount',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
        },
        {
          name: 'hashCode',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'min',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'signum',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'reverseBytes',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'Integer',
            },
          ],
        },
        {
          name: 'byteValue',
          returnType: 'byte',
        },
        {
          name: 'shortValue',
          returnType: 'short',
        },
        {
          name: 'intValue',
          returnType: 'int',
        },
        {
          name: 'longValue',
          returnType: 'long',
        },
        {
          name: 'floatValue',
          returnType: 'float',
        },
        {
          name: 'doubleValue',
          returnType: 'double',
        },
        {
          name: 'valueOf',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toHexString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'decode',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'compare',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'reverse',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'toUnsignedLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'parseInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'sum',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'compareUnsigned',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toUnsignedString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toUnsignedString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'toOctalString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'toBinaryString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseUnsignedInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseUnsignedInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'parseUnsignedInt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'getInteger',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'getInteger',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'getInteger',
          returnType: 'Integer',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'Integer',
            },
          ],
        },
        {
          name: 'divideUnsigned',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'remainderUnsigned',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'highestOneBit',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'lowestOneBit',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'rotateLeft',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'rotateRight',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    Long: {
      methods: [
        {
          name: 'numberOfLeadingZeros',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'numberOfTrailingZeros',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'bitCount',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toString',
          returnType: 'String',
        },
        {
          name: 'hashCode',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'min',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'signum',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'reverseBytes',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'Long',
            },
          ],
        },
        {
          name: 'getLong',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'getLong',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'getLong',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'Long',
            },
          ],
        },
        {
          name: 'byteValue',
          returnType: 'byte',
        },
        {
          name: 'shortValue',
          returnType: 'short',
        },
        {
          name: 'intValue',
          returnType: 'int',
        },
        {
          name: 'longValue',
          returnType: 'long',
        },
        {
          name: 'floatValue',
          returnType: 'float',
        },
        {
          name: 'doubleValue',
          returnType: 'double',
        },
        {
          name: 'valueOf',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'toHexString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'decode',
          returnType: 'Long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'compare',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'reverse',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'sum',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'compareUnsigned',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'toUnsignedString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'toUnsignedString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'toOctalString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'toBinaryString',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'parseLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'divideUnsigned',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'remainderUnsigned',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'highestOneBit',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'lowestOneBit',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'rotateLeft',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'rotateRight',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseUnsignedLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'parseUnsignedLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'parseUnsignedLong',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    Math: {
      methods: [
        {
          name: 'abs',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'abs',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'abs',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'abs',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'sin',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'cos',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'tan',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'atan2',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'sqrt',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'log',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'log10',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'pow',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'exp',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'min',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'min',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'float',
            },
          ],
        },
        {
          name: 'min',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'min',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'float',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'max',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'floor',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'ceil',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'rint',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'addExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'addExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'decrementExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'decrementExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'incrementExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'incrementExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'multiplyExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'multiplyExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'multiplyExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'multiplyHigh',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'negateExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'negateExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'subtractExact',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'subtractExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'fma',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'float',
            },
            {
              name: 'arg2',
              type: 'float',
            },
          ],
        },
        {
          name: 'fma',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
            {
              name: 'arg2',
              type: 'double',
            },
          ],
        },
        {
          name: 'copySign',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'float',
            },
          ],
        },
        {
          name: 'copySign',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'signum',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'signum',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'scalb',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'scalb',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'getExponent',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'getExponent',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'floorMod',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'floorMod',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'floorMod',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'asin',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'acos',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'atan',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'toRadians',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'toDegrees',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'cbrt',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'IEEEremainder',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'round',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'round',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'random',
          returnType: 'double',
        },
        {
          name: 'toIntExact',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'multiplyFull',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'floorDiv',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'floorDiv',
          returnType: 'long',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
            {
              name: 'arg1',
              type: 'long',
            },
          ],
        },
        {
          name: 'floorDiv',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'ulp',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'ulp',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'sinh',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'cosh',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'tanh',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'hypot',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'expm1',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'log1p',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'nextAfter',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'nextAfter',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
            {
              name: 'arg1',
              type: 'double',
            },
          ],
        },
        {
          name: 'nextUp',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'nextUp',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'nextDown',
          returnType: 'float',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'nextDown',
          returnType: 'double',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    Object: {
      methods: [
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    List: {
      methods: [
        {
          name: 'get',
          returnType: 'Object',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'contains',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'size',
          returnType: 'int',
        },
      ],
    },
    Collection: {
      methods: [
        {
          name: 'contains',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'size',
          returnType: 'int',
        },
      ],
    },
    Set: {
      methods: [
        {
          name: 'contains',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'size',
          returnType: 'int',
        },
      ],
    },
    String: {
      methods: [
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'length',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'getChars',
          returnType: 'void',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'char[]',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'compareTo',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'indexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'indexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'indexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'indexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'long',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'double',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'float',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char[]',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'boolean',
            },
          ],
        },
        {
          name: 'valueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char[]',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
          ],
        },
        {
          name: 'isEmpty',
          returnType: 'boolean',
        },
        {
          name: 'charAt',
          returnType: 'char',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'codePointAt',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'codePointBefore',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'codePointCount',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'offsetByCodePoints',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'getBytes',
          returnType: 'byte[]',
          params: [
            {
              name: 'arg0',
              type: 'Charset',
            },
          ],
        },
        {
          name: 'getBytes',
          returnType: 'void',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'byte[]',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'getBytes',
          returnType: 'byte[]',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'getBytes',
          returnType: 'byte[]',
        },
        {
          name: 'contentEquals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'StringBuffer',
            },
          ],
        },
        {
          name: 'contentEquals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
          ],
        },
        {
          name: 'equalsIgnoreCase',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'regionMatches',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'boolean',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'String',
            },
            {
              name: 'arg3',
              type: 'int',
            },
            {
              name: 'arg4',
              type: 'int',
            },
          ],
        },
        {
          name: 'regionMatches',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'String',
            },
            {
              name: 'arg2',
              type: 'int',
            },
            {
              name: 'arg3',
              type: 'int',
            },
          ],
        },
        {
          name: 'compareToIgnoreCase',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'startsWith',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'startsWith',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'endsWith',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'lastIndexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'lastIndexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'lastIndexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'lastIndexOf',
          returnType: 'int',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'substring',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'substring',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'subSequence',
          returnType: 'CharSequence',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'concat',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'replace',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'CharSequence',
            },
          ],
        },
        {
          name: 'replace',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char',
            },
            {
              name: 'arg1',
              type: 'char',
            },
          ],
        },
        {
          name: 'matches',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'contains',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
          ],
        },
        {
          name: 'replaceFirst',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'String',
            },
          ],
        },
        {
          name: 'replaceAll',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'String',
            },
          ],
        },
        {
          name: 'split',
          returnType: 'String[]',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
          ],
        },
        {
          name: 'split',
          returnType: 'String[]',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'int',
            },
          ],
        },
        {
          name: 'join',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'CharSequence[]',
            },
          ],
        },
        {
          name: 'join',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'CharSequence',
            },
            {
              name: 'arg1',
              type: 'Iterable',
            },
          ],
        },
        {
          name: 'toLowerCase',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'Locale',
            },
          ],
        },
        {
          name: 'toLowerCase',
          returnType: 'String',
        },
        {
          name: 'toUpperCase',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'Locale',
            },
          ],
        },
        {
          name: 'toUpperCase',
          returnType: 'String',
        },
        {
          name: 'trim',
          returnType: 'String',
        },
        {
          name: 'strip',
          returnType: 'String',
        },
        {
          name: 'stripLeading',
          returnType: 'String',
        },
        {
          name: 'stripTrailing',
          returnType: 'String',
        },
        {
          name: 'isBlank',
          returnType: 'boolean',
        },
        {
          name: 'lines',
          returnType: 'Stream',
        },
        {
          name: 'chars',
          returnType: 'IntStream',
        },
        {
          name: 'codePoints',
          returnType: 'IntStream',
        },
        {
          name: 'toCharArray',
          returnType: 'char[]',
        },
        {
          name: 'format',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'String',
            },
            {
              name: 'arg1',
              type: 'Object[]',
            },
          ],
        },
        {
          name: 'format',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'Locale',
            },
            {
              name: 'arg1',
              type: 'String',
            },
            {
              name: 'arg2',
              type: 'Object[]',
            },
          ],
        },
        {
          name: 'copyValueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char[]',
            },
            {
              name: 'arg1',
              type: 'int',
            },
            {
              name: 'arg2',
              type: 'int',
            },
          ],
        },
        {
          name: 'copyValueOf',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'char[]',
            },
          ],
        },
        {
          name: 'intern',
          returnType: 'String',
        },
        {
          name: 'repeat',
          returnType: 'String',
          params: [
            {
              name: 'arg0',
              type: 'int',
            },
          ],
        },
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
    'String[]': {
      methods: [
        {
          name: 'equals',
          returnType: 'boolean',
          params: [
            {
              name: 'arg0',
              type: 'Object',
            },
          ],
        },
        {
          name: 'hashCode',
          returnType: 'int',
        },
        {
          name: 'toString',
          returnType: 'String',
        },
      ],
    },
  },
};
