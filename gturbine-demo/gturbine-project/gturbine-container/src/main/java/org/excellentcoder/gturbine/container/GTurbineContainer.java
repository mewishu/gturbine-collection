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
package org.excellentcoder.gturbine.container;

import org.excellentcoder.gturbine.archive.Archive;

import java.util.List;

/**
 * GT容器
 * 
 * @author xbyan
 * @version $Id: GTurbineContainer.java, v 0.1 2019-12-04 8:13 PM xbyan Exp $$
 */
public class GTurbineContainer {
    /**
     * 构造GT容器，需要提供容器的位置，以及bizClassLoader
     *
     * @param serverArchive
     * @param pluginArchives 
     * @param bizArchives 
     * @param containerLoader
     */
    public GTurbineContainer(Archive serverArchive, List<Archive> pluginArchives,
                             List<Archive> bizArchives, ClassLoader containerLoader) {

    }

    /**
     * <pre>
     * 构造Pandora容器，需要提供容器的位置，以及bizClassLoader
     *
     * </pre>
     *
     * @throws Exception
     */
    public void start() throws Exception {
        int iii = 0;
    }

}