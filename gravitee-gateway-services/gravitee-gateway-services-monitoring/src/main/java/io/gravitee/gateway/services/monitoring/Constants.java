/**
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
package io.gravitee.gateway.services.monitoring;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public final class Constants {

    private Constants() {}  // can't construct

    /** JVM vendor info. */
    public static final String JVM_VENDOR = System.getProperty("java.vm.vendor");
    public static final String JVM_VERSION = System.getProperty("java.vm.version");
    public static final String JVM_NAME = System.getProperty("java.vm.name");

    /** The value of <tt>System.getProperty("java.version")</tt>. **/
    public static final String JAVA_VERSION = System.getProperty("java.version");

    /** The value of <tt>System.getProperty("os.name")</tt>. **/
    public static final String OS_NAME = System.getProperty("os.name");
    /** True iff running on Linux. */
    public static final boolean LINUX = OS_NAME.startsWith("Linux");
    /** True iff running on Windows. */
    public static final boolean WINDOWS = OS_NAME.startsWith("Windows");
    /** True iff running on SunOS. */
    public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");
    /** True iff running on Mac OS X */
    public static final boolean MAC_OS_X = OS_NAME.startsWith("Mac OS X");
    /** True iff running on FreeBSD */
    public static final boolean FREE_BSD = OS_NAME.startsWith("FreeBSD");

    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String OS_VERSION = System.getProperty("os.version");
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");
}
