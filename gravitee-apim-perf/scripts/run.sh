#!/usr/bin/env bash
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
set -eo pipefail

############################################################
# Help                                                     #
############################################################
function help()
{
   # Display Help
   echo "Runs a Gravitee APIM K6 test"
   echo
   echo "Syntax: bash [-x] ./run.sh -f [file_path] [-d|v|r|o|h]"
   echo "options:"
   echo "  f     ⚠️ Mandatory. Path to the test to run"
   echo "  d     Developer mode. Build the project before running k6 test"
   echo "  v     K6 Verbose mode. Enable --verbose and --http-debug on k6 command"
   echo "  r     Prometheus Remote URL. Useful when reporting to prometheus. See option -o. If not provided, use the one from config.json"
   echo "  o     K6 output mode. If not provided, use the one from config.json. Possible values are 'cloud, csv, experimental-prometheus-rw, influxdb, json, statsd'"
   echo "  h     Display the help"
   echo
   echo "💡 Note: you can configure the K6 context by update the config.json file."
}

############################################################
# Main program                                             #
############################################################

developer_mode=false
verbose=false
file_path=''
remote_url=''
output_mode=''

### Parse variables
shift $((OPTIND-1))
while getopts hdvf:r:o: options
do
   case $options in
      f) # file to test
        file_path=${OPTARG}
        ;;
      h) # display Help
         help
         exit;;
      d) # developer mode
        developer_mode=true
        echo "Developer mode enabled"
        ;;
      v) # verbose
        verbose=true
        echo "Verbose mode enabled"
        ;;
      r) # prometheus remote url
        remote_url=${OPTARG}
        echo "K6 Remote URL ${remote_url}"
        ;;
      o) # K6 output mode
        output_mode=${OPTARG}
        echo "K6 output mode ${output_mode}"
        ;;
      *) # Invalid option
         echo "Error: Invalid option"
         exit;;
   esac
done

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
echo "Script directory: "$SCRIPT_DIR
CONFIG_DIR=${CONFIG_DIR:-$SCRIPT_DIR}
echo "Config directory: "$CONFIG_DIR

### Check tooling
if [[ -z "$(whereis k6)" && -z "$(which k6)" ]];
then
    echo "[ERROR] k6 not installed, please install k6"
    exit 1
fi

if [[ -z "$(whereis yarn)" && -z "$(which yarn)" ]];
then
    echo "[ERROR] yarn not installed, please install yarn"
    exit 1
fi

### Check file_path
if [[ -z "${file_path}" ]];
then
    echo "file_path must be set thanks to -f option"
    help
    exit 2
fi

export K6_OPTIONS=$(jq -c '.' "${CONFIG_DIR}/config.json")

cd "$SCRIPT_DIR/.."

### If developer mode enabled, build typescript files.
if [ ${developer_mode} = true ]
then
  yarn webpack
fi

echo "Current K6 Context:"
env | grep -e "^K6_.*=.*" || true

### If verbose mode enabled, then prepare K6 options.
VERBOSE_OPTIONS=""
if [ ${verbose} = true ]
then
  VERBOSE_OPTIONS="--http-debug --verbose"
fi

### Export K6_PROMETHEUS_REMOTE_URL to be used by K6 to report stats to Prometheus
if [[ "" == "${remote_url}" ]];
then
    echo "Prometheus remote is not set (option -r), using value from config.json"
    export K6_PROMETHEUS_RW_SERVER_URL=$(jq --raw-output '.k6.prometheusRemoteUrl' "${CONFIG_DIR}/config.json")
else
  export K6_PROMETHEUS_RW_SERVER_URL=$remote_url
fi

### If output_mode option not defined, use value from config.json
if [[ "" == "${output_mode}" ]];
then
    echo "K6 Output mode is not set (option -o), using value from config.json"
    output_mode=$(jq -r '.k6.outputMode' "${CONFIG_DIR}/config.json")
fi
echo $K6_PROMETHEUS_RW_SERVER_URL
### Run K6
k6 run --include-system-env-vars ${VERBOSE_OPTIONS} -o ${output_mode} -e K6_PROMETHEUS_RW_TREND_STATS="p(99),p(95),p(90),avg" "${file_path}"

exit $?