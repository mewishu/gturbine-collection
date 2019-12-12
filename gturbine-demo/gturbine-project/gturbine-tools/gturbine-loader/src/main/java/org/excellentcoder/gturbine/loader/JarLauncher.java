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
package org.excellentcoder.gturbine.loader;

import org.excellentcoder.gturbine.archive.Archive;

/**
 * spring boot应用打包之后，生成一个fat jar，里面包含了应用依赖的jar包，还有Spring boot loader相关的类
 * Fat jar的启动Main函数是JarLauncher，它负责创建一个LaunchedURLClassLoader来加载/lib下面的jar，并以一个新线程启动应用的Main函数。
 * 以fat jar运行启动方法:
 * mvn clean package
 * java -jar target/demo-classloader-context-0.0.1-SNAPSHOT.jar
 *
 * 具体实现原理: gturbine-maven-plugin打包生成的MANIFEST.MF文件中会指定:
 * Main-Class: org.springframework.boot.loader.JarLauncher
 * Start-Class: com.example.SpringBootDemoApplication
 * (Main-Class受layout属性的控制, 设置<layout>ZIP</layout>时Main-Class为org.springframework.boot.loader.PropertiesLauncher，具体layout值对应Main-Class关系如下：
 *
 * JAR，即通常的可执行jar
 * Main-Class: org.springframework.boot.loader.JarLauncher
 *
 * WAR，即通常的可执行war，需要的servlet容器依赖位于WEB-INF/lib-provided
 * Main-Class: org.springframework.boot.loader.warLauncher
 *
 * ZIP，即DIR，类似于JAR
 * Main-Class: org.springframework.boot.loader.PropertiesLauncher
 *
 * MODULE，将所有的依赖库打包（scope为provided的除外），但是不打包Spring Boot的任何Launcher
 * NONE，将所有的依赖库打包，但是不打包Spring Boot的任何Launcher)
 *
 *
 * 所以JarLauncher是jar启动的Main函数, Start-Class是com.example.SpringBootDemoApplication，这个是我们应用自己的Main函数。
 *
 *
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarLauncher extends ExecutableArchiveLauncher {

    static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";

    static final String BOOT_INF_LIB     = "BOOT-INF/lib/";

    public JarLauncher() {
    }

    protected JarLauncher(Archive archive) {
        super(archive);
    }

    @Override
    protected boolean isNestedArchive(Archive.Entry entry) {
        if (entry.isDirectory()) {
            return entry.getName().equals(BOOT_INF_CLASSES);
        }
        return entry.getName().startsWith(BOOT_INF_LIB);
    }

    public static void main(String[] args) throws Exception {
        new JarLauncher().launch(args);
    }

}