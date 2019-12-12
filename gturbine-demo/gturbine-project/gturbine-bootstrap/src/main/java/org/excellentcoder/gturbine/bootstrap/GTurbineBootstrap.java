/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.excellentcoder.gturbine.bootstrap;

import org.excellentcoder.gturbine.loader.ClasspathLauncher;

import java.net.URL;
import java.net.URLClassLoader;

public class GTurbineBootstrap {

    public static void main(String[] args) {
        run(args);
    }

    /**
     * 启动GTurbine。通常在应用的main函数的第一行调用。
     *
     * @param args
     *            应用main函数的args
     */
    public static void run(String[] args) {
        URL[] urls = getURLClassPath(GTurbineBootstrap.class.getClassLoader());
        if (urls == null) {
            throw new IllegalStateException(
                "Can not find urls from the ClassLoader of GTurbineBootstrap. ClassLoader: "
                        + GTurbineBootstrap.class.getClassLoader());
        }

        ClasspathLauncher.launch(args, determineMainApplicationClass().getName(), urls);
    }

    /**
     * 从classloader中获取urls
     *
     * @see <a href="https://stackoverflow.com/questions/49557431/how-to-safely-access-the-urls-of-all-resource-files-in-the-classpath-in-java-9-1">stackoverflow.com</a>
     *
     * @param classLoader
     * @return
     */
    private static URL[] getURLClassPath(ClassLoader classLoader) {
        // 暂不支持jdk9
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        return null;
    }

    /**
     * 从栈里获取原始的main类
     *
     * @return
     */
    private static Class<?> determineMainApplicationClass() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            StackTraceElement mainElement = stackTrace[stackTrace.length - 1];

            return Class.forName(mainElement.getClassName());

        } catch (ClassNotFoundException ex) {
            // Swallow and continue
        }

        return null;
    }
}
