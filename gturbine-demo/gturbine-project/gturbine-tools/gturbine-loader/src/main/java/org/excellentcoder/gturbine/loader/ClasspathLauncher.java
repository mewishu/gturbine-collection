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

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.excellentcoder.gturbine.archive.Archive;
import org.excellentcoder.gturbine.archive.ExplodedArchive;
import org.excellentcoder.gturbine.archive.JarFileArchive;
import org.excellentcoder.gturbine.archive.jar.JarFile;
import org.excellentcoder.gturbine.archive.util.ResourceUtils;
import org.excellentcoder.gturbine.archive.util.SystemPropertyUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类路径启动器
 *
 * @author xbyan
 * @version $Id: ClasspathLauncher.java, v 0.1 2019-12-05 10:20 AM xbyan Exp $$
 */
public class ClasspathLauncher extends Launcher {

    /** GT启动类 */
    private static final String GT_BOOT_MAIN               = "org.excellentcoder.gturbine.container.GTurbineContainer";

    /** gt容器位置 */
    private static final String GTURBINE_LOCATION          = "gturbine.location";

    /** 内核、插件标识资源文件 */
    private static final String GTURBINE_PROPERTIES        = "org/excellentcoder/gturbine/gturbine.properties";
    private static final String PLUGIN_GTURBINE_PROPERTIES = "org/excellentcoder/gturbine/plugin.gturbine.properties";

    private final URL[]         urls;

    /**
     * 根据urls构造launcher
     *
     * @param urls 类加载路径
     */
    public ClasspathLauncher(URL[] urls) {
        this.urls = urls;
    }

    /**
     * 以Main函数ClassLoader的urls重新构造一个 ClassLoader，加载GTurbine，再重新加载Main函数，
     * 解决部分类因为类型检测被提前加载的问题
     *
     * @param args  入参
     * @param mainClass  主入口类
     * @param urls 类加载路径
     */
    public static void launch(String[] args, String mainClass, URL[] urls) {

        // reLaunch 里以一个新线程，新classloader启动main函数，并等待新的main函数线程退出
        // reLaunch(args, mainClass, createClassLoader(urls));
        try {

            new ClasspathLauncher(urls).launch(args);
        } catch (Throwable e) {
            throw new RuntimeException("load gturbine error!", e);
        }

        // 执行到这里，新启动的main线程已经退出了，可以直接退出进程
        System.exit(0);
    }

    @Override
    protected void launch(String[] args) throws Exception {
        JarFile.registerUrlProtocolHandler();

        // 1. 构建classloader
        List<Archive> archives = getClassPathArchives();
        ClassLoader classLoader = createClassLoader(archives);
        Thread.currentThread().setContextClassLoader(classLoader);

        // 2. 根据classloader创建容器
        Object container = createContainer(classLoader);

        // 3. 启动容器
        MethodUtils.invokeMethod(container, "start", null);
    }

    @Override
    protected ClassLoader createClassLoader(URL[] urls) throws Exception {
        return new ClasspathURLClassLoader(urls, null);
    }

    @Override
    protected String getMainClass() throws Exception {
        return GT_BOOT_MAIN;
    }

    @Override
    protected List<Archive> getClassPathArchives() throws Exception {
        List<Archive> archives = new ArrayList<>();

        for (URL url : urls) {
            if (ResourceUtils.isFileURL(url)) {
                File file = ResourceUtils.getFile(url);
                if (file.isDirectory()) {
                    archives.add(new ExplodedArchive(file));
                } else {
                    archives.add(new JarFileArchive(file));
                }
            }
        }

        return archives;
    }

    /**
     * 创建容器
     * 
     * @return
     */
    private Object createContainer(ClassLoader classLoader) throws Exception {
        // 1. 根据资源文件创建内核归档文件
        Archive serverArchive = createServerArchive(classLoader);

        // 2. 根据资源文件创建插件归档文件、应用归档文件
        List<Archive> gturbinePluginArchives = createArchiveByResources(classLoader,
            PLUGIN_GTURBINE_PROPERTIES);

        // 3. 反射调用构造函数创建容器
        return ConstructorUtils.invokeConstructor(classLoader.loadClass(getMainClass()),
            serverArchive, gturbinePluginArchives, new ArrayList<>(), classLoader);
    }

    /**
     * 创建内核归档文件
     * 
     * @param classLoader
     * @return
     */
    private Archive createServerArchive(ClassLoader classLoader) throws IOException {
        String gturbineJarPath = SystemPropertyUtils.getProperty(GTURBINE_LOCATION);
        if (!StringUtils.isEmpty(gturbineJarPath)) {
            File gturbineFile = new File(gturbineJarPath);
            Assert.isTrue(gturbineFile.exists(),
                "Please make sure right full file path in '-Dgturbine.location='");
            return gturbineFile.isDirectory() ? new ExplodedArchive(gturbineFile)
                : new JarFileArchive(gturbineFile);
        } else {
            List<Archive> serverArchives = createArchiveByResources(classLoader,
                GTURBINE_PROPERTIES);
            Assert
                .notEmpty(
                    serverArchives,
                    "Can not load gturbine-all.jar! please check '-Dgrurbine.location=' or maven dependencies if there contains gturbine-all.jar!");
            Assert.isTrue(serverArchives.size() == 1,
                "Found more than one gturbine-all.jar from classpath, please check your config! urls:"
                        + Arrays.toString(urls));

            return serverArchives.get(0);
        }
    }

    /**
     * 从指定的classloader找到对应的资源
     * 
     * @param loader
     * @param locationPattern
     * @return
     */
    private List<Archive> createArchiveByResources(ClassLoader loader, String locationPattern) {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(loader);
            Resource[] resources = resolver.getResources(locationPattern);

            return Arrays.stream(resources).map(r -> {
                try {
                    return r.getFile().isDirectory() ? new ExplodedArchive(r.getFile())
                        : new JarFileArchive(r.getFile());
                } catch (IOException e) {
                    throw new IllegalStateException(
                        "Error reading source '" + locationPattern + "'");
                }
            }).collect(Collectors.toList());

        } catch (IOException ex) {
            throw new IllegalStateException("Error reading source '" + locationPattern + "'");
        }
    }
}
