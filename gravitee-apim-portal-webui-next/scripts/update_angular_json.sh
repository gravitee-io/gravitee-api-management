#!/bin/sh
# Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

# shellcheck disable=SC2039
LOCALE_REGEX='^\./src/locale/messages\.(.*)\.'
LOCALES_JSON=""

shopt -s nullglob
TRANSLATIONS=(./src/locale/messages.*.*)

[[ ${#TRANSLATIONS[@]} == 0 ]] && echo "ERROR: No translations with locales found in 'src/locale'" && exit 1

for file in "${TRANSLATIONS[@]}"; do
    if [[ $LOCALES_JSON != "" ]] # No longer the first file
    then
      LOCALES_JSON+="," # Add comma to separate multiple locales
    fi
    FORMATTED_NAME=$(echo "$file" | sed 's/\.\///g') # Replace './' with ''
    echo "Adding translation: $FORMATTED_NAME"
    [[ $file =~ $LOCALE_REGEX ]] # Get locale from regex group
    LOCALES_JSON+="\"${BASH_REMATCH[1]}\": { \"translation\": \"${FORMATTED_NAME}\"}"
done

# Replace i18n locales value in 'angular.json'
jq ".projects.\"gravitee-apim-portal-webui-next\".i18n.locales = {${LOCALES_JSON}}" angular.json > angular.json.tmp && cp angular.json.tmp angular.json && rm angular.json.tmp && echo "SUCCESS: Locale configuration has been updated" || echo "ERROR: Unable to update locale configuration"
