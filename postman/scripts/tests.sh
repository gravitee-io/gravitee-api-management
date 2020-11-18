#!/usr/bin/env bash
#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

POSTMAN_DIR=../postman

set -e;

if [ -z "$1" ]
then
	echo "Using default path: $POSTMAN_DIR"
else
	echo "Using custom path: $1"
	POSTMAN_DIR=$1
fi

for f in $POSTMAN_DIR/test/*;do if [[ -f $f ]]; then newman run $f -e $POSTMAN_DIR/env/Gravitee.io-Localhost-Environment.json --bail; fi; done;