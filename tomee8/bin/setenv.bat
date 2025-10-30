@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem Environment variables for TomEE
rem UTF-8 encoding for Japanese characters
set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8"
set "JAVA_OPTS=%JAVA_OPTS% -Duser.language=ja"
set "JAVA_OPTS=%JAVA_OPTS% -Duser.country=JP"
set "JAVA_OPTS=%JAVA_OPTS% -Dconsole.encoding=UTF-8"
set "JAVA_OPTS=%JAVA_OPTS% -Dsun.stdout.encoding=UTF-8"
set "JAVA_OPTS=%JAVA_OPTS% -Dsun.stderr.encoding=UTF-8"

rem Memory settings
set "JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx1024m"

