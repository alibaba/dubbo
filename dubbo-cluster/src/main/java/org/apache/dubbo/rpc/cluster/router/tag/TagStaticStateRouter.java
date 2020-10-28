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
package org.apache.dubbo.rpc.cluster.router.tag;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.BitList;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.RouterCache;
import org.apache.dubbo.rpc.cluster.router.tag.model.TagRouterRule;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

/**
 * TagRouter, "application.tag-router"
 */
public class TagStaticStateRouter extends AbstractStateRouter {
    public static final String NAME = "TAG_ROUTER";
    private static final int TAG_ROUTER_DEFAULT_PRIORITY = 100;
    private static final Logger logger = LoggerFactory.getLogger(TagStaticStateRouter.class);
    private static final String RULE_SUFFIX = ".tag-router";
    private static final String NO_TAG = "noTag";

    private TagRouterRule tagRouterRule;

    public TagStaticStateRouter(URL url) {
        super(url);
        this.priority = TAG_ROUTER_DEFAULT_PRIORITY;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache routerCache, URL url, Invocation invocation)
        throws RpcException {

        String tag = StringUtils.isEmpty(invocation.getAttachment(TAG_KEY)) ? url.getParameter(TAG_KEY) :
            invocation.getAttachment(TAG_KEY);
        if (StringUtils.isEmpty(tag)) {
            tag = NO_TAG;
        }

        ConcurrentHashMap<String, BitList<Invoker>> pool = routerCache.getAddrPool();
        BitList res = pool.get(tag);
        if (res == null) {
            return invokers;
        }
        return invokers.intersect((BitList)res, invokers.getUnmodifiableList());
    }


    @Override
    public boolean isRuntime() {
        return tagRouterRule != null && tagRouterRule.isRuntime();
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    public boolean isForce() {
        // FIXME
        return false;
    }

    @Override
    public String getName() {
        return "TagStatic";
    }

    @Override
    public boolean shouldRePool() {
        return false;
    }

    @Override
    public <T> RouterCache pool(List<Invoker<T>> invokers) {

        RouterCache routerCache = new RouterCache();
        ConcurrentHashMap<String, BitList<Invoker<T>>> addrPool = new ConcurrentHashMap<>();

        for (int index = 0; index < invokers.size(); index++) {
            Invoker<T> invoker = invokers.get(index);
            String tag = invoker.getUrl().getParameter(TAG_KEY);
            if (StringUtils.isEmpty(tag)) {
                BitList<Invoker<T>> noTagList = addrPool.putIfAbsent(NO_TAG, new BitList<>(invokers, true));
                if (noTagList == null) {
                    noTagList = addrPool.get(NO_TAG);
                }
                noTagList.addIndex(index);
            } else {
                BitList<Invoker<T>> list = addrPool.putIfAbsent(tag, new BitList<>(invokers, true));
                if (list == null) {
                    list = addrPool.get(tag);
                }
                list.addIndex(index);
            }
        }

        routerCache.setAddrPool((ConcurrentHashMap)addrPool);

        return routerCache;
    }


    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }

        pool(invokers);
    }

}
