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
package org.apache.dubbo.configcenter.support.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.mapping.MappingChangedEvent;
import org.apache.dubbo.mapping.MappingListener;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import org.apache.zookeeper.data.Stat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 *
 */
public class ZookeeperDynamicConfiguration extends TreePathDynamicConfiguration {

    private Executor executor;
    // The final root path would be: /configRootPath/"config"
    private String rootPath;
    private final ZookeeperClient zkClient;
    private CountDownLatch initializedLatch;

    private CacheListener cacheListener;
    private URL url;

    private Map<String, MappingChildListener> listenerMap = new ConcurrentHashMap<>();


    ZookeeperDynamicConfiguration(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        this.url = url;
        rootPath = getRootPath(url);

        initializedLatch = new CountDownLatch(1);
        this.cacheListener = new CacheListener(rootPath, initializedLatch);
        this.executor = Executors.newFixedThreadPool(1, new NamedThreadFactory(this.getClass().getSimpleName(), true));

        zkClient = zookeeperTransporter.connect(url);
        zkClient.addDataListener(rootPath, cacheListener, executor);
        try {
            // Wait for connection
            long timeout = url.getParameter("init.timeout", 5000);
            boolean isCountDown = this.initializedLatch.await(timeout, TimeUnit.MILLISECONDS);
            if (!isCountDown) {
                throw new IllegalStateException("Failed to receive INITIALIZED event from zookeeper, pls. check if url "
                        + url + " is correct");
            }
        } catch (InterruptedException e) {
            logger.warn("Failed to build local cache for config center (zookeeper)." + url);
        }
    }

    /**
     * @param key e.g., {service}.configurators, {service}.tagrouters, {group}.dubbo.properties
     * @return
     */
    @Override
    public String getInternalProperty(String key) {
        return zkClient.getContent(buildPathKey("", key));
    }

    @Override
    protected void doClose() throws Exception {
        zkClient.close();
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        zkClient.create(pathKey, content, false);
        return true;
    }

    @Override
    public boolean publishConfigCas(String key, String group, String content, Object ticket) {
        try {
            if (ticket != null && !(ticket instanceof Stat)) {
                throw new IllegalArgumentException("zookeeper publishConfigCas requires stat type ticket");
            }
            String pathKey = buildPathKey(group, key);
            zkClient.createOrUpdate(pathKey, content, false, ticket == null ? 0 : ((Stat) ticket).getVersion());
            return true;
        } catch (Exception e) {
            logger.warn("zookeeper publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        Set<String> appNameSet = new HashSet<>();
        String path = toRootDir() + serviceKey;

        if (null == listenerMap.get(path)) {
            zkClient.create(path, false);
            appNameSet.addAll(addServiceMappingListener(path, serviceKey, listener));
        } else {
            appNameSet.addAll(zkClient.getChildren(path));
        }

        return appNameSet;
    }

    String toRootDir() {
        if (rootPath.equals(PATH_SEPARATOR)) {
            return rootPath;
        }
        return rootPath + PATH_SEPARATOR;
    }

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        return zkClient.getContent(pathKey);
    }

    @Override
    public ConfigItem getConfigItem(String key, String group) {
        String pathKey = buildPathKey(group, key);
        return zkClient.getConfigItem(pathKey);
    }

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        zkClient.delete(pathKey);
        return true;
    }

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        return zkClient.getChildren(groupPath);
    }

    @Override
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        cacheListener.addListener(pathKey, listener);
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        cacheListener.removeListener(pathKey, listener);
    }

    @Override
    public boolean isSupportCas() {
        return true;
    }

    private List<String> addServiceMappingListener(String path, String serviceKey, MappingListener listener) {
        MappingChildListener mappingChildListener = listenerMap.computeIfAbsent(path, _k -> new MappingChildListener(serviceKey, path));
        mappingChildListener.addListener(listener);
        return zkClient.addChildListener(path, mappingChildListener);
    }

    private static class MappingChildListener implements ChildListener {
        private String serviceKey;
        private String path;
        private Set<MappingListener> listeners;

        public MappingChildListener(String serviceKey, String path) {
            this.serviceKey = serviceKey;
            this.path = path;
            this.listeners = new HashSet<>();
        }

        public void addListener(MappingListener listener) {
            this.listeners.add(listener);
        }

        @Override
        public void childChanged(String path, List<String> children) {
            MappingChangedEvent event = new MappingChangedEvent();
            event.setServiceKey(serviceKey);
            event.setApps(null != children ? new HashSet<>(children) : null);
            listeners.forEach(mappingListener -> mappingListener.onEvent(event));
        }
    }
}
