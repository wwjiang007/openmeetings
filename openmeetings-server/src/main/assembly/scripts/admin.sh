#!/bin/bash
# #############################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #############################################
export OM_HOME=$(cd $(dirname "$0"); pwd) 
export OM_CONTEXT=openmeetings

export CLASSPATH=$OM_HOME/*:$OM_HOME/lib/*:$OM_HOME/webapps/${OM_CONTEXT}/WEB-INF/lib/*:$OM_HOME/webapps/${OM_CONTEXT}/WEB-INF:$OM_HOME/webapps/${OM_CONTEXT}/WEB-INF/classes

java -cp "$CLASSPATH" -Dom_home=${OM_HOME} -Dcontext=${OM_CONTEXT} org.apache.openmeetings.cli.Admin $*

