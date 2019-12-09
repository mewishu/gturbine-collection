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
import org.excellentcoder.gturbine.loader.archive.Archive;
import org.excellentcoder.gturbine.loader.archive.ExplodedArchive;
import org.excellentcoder.gturbine.loader.archive.JarFileArchive;
import org.excellentcoder.gturbine.loader.jar.JarFile;
import org.excellentcoder.gturbine.loader.util.ResourceUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 重新启动器
 *
 * @author xbyan
 * @version $Id: ContainerLauncher.java, v 0.1 2019-12-05 10:20 AM xbyan Exp $$
 */
public class ContainerLauncher extends Launcher {

    /** GT启动类 */
    private static final String GT_BOOT_MAIN = "org.excellentcoder.gturbine.container.GTurbineContainer";

    private final URL[]         urls;

    /** 内核归档文件 */
    private Archive             serverArchive;
    /** 插件归档文件列表 */
    private List<Archive>       pluginArchives;
    /** 应用归档文件列表 */
    private List<Archive>       bizArchives;

    /**
     * 根据urls构造launcher
     *
     * @param urls 类加载路径
     */
    public ContainerLauncher(URL[] urls) {
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

            new ContainerLauncher(urls).launch(args);
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

        // 2. 创建容器
        Class<?> targetClass = classLoader.loadClass(getMainClass());
        Object container = ConstructorUtils.invokeConstructor(targetClass, serverArchive,
            pluginArchives, bizArchives, classLoader);

        // 3. 启动容器
        MethodUtils.invokeMethod(container, "start", null);
    }

    @Override
    protected ClassLoader createClassLoader(URL[] urls) throws Exception {
        return new ContainerURLClassLoader(urls, null);
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
}
