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
package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.metadata.InstanceMetadataChangedListener;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.dns.util.DNSClientConst;
import org.apache.dubbo.registry.dns.util.DNSResolver;
import org.apache.dubbo.registry.dns.util.ResolveResult;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DNSServiceDiscoveryTest {

    @BeforeEach
    public void setup() {
        ApplicationModel.reset();
        ApplicationConfig applicationConfig = new ApplicationConfig("Test");
        ApplicationModel.getConfigManager().setApplication(applicationConfig);
    }

    @AfterEach
    public void destroy() {
        ApplicationModel.reset();
    }

    @Test
    public void testProvider() throws Exception {
        ServiceDiscovery dnsServiceDiscovery = new DNSServiceDiscovery();

        URL registryURL = URL.valueOf("dns://");
        dnsServiceDiscovery.initialize(registryURL);

        assertEquals(registryURL, dnsServiceDiscovery.getUrl());

        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "localhost", 12345);
        serviceInstance.getMetadata().put("a", "b");

        dnsServiceDiscovery.register(serviceInstance);

        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();
        InstanceMetadataChangedListener changeListener = Mockito.mock(InstanceMetadataChangedListener.class);

        String metadataString = metadataService
                .getAndListenInstanceMetadata("test", changeListener);

        assertEquals(JSONObject.toJSONString(serviceInstance.getMetadata()), metadataString);
        assertEquals(serviceInstance, dnsServiceDiscovery.getLocalInstance());

        dnsServiceDiscovery.unregister(serviceInstance);

        Mockito.verify(changeListener, Mockito.times(1)).onEvent(Mockito.any());

        metadataService.getInstanceMetadataChangedListenerMap().clear();
        metadataService.exportInstanceMetadata(null);

        dnsServiceDiscovery.destroy();

    }

    @Test
    public void testConsumer() throws Exception {
        DNSServiceDiscovery dnsServiceDiscovery = new DNSServiceDiscovery();

        URL registryURL = URL.valueOf("dns://")
                .addParameter(DNSClientConst.POLLING_CYCLE, 100)
                .addParameter(DNSClientConst.ECHO_POLLING_CYCLE, 100);
        dnsServiceDiscovery.initialize(registryURL);

        WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();
        ServiceInstance serviceInstance = new DefaultServiceInstance("TestService", "localhost", 12345);
        serviceInstance.getMetadata().put("a", "b");

        dnsServiceDiscovery.register(serviceInstance);

        int port = NetUtils.getAvailablePort();
        ApplicationModel.getApplicationConfig().setMetadataServicePort(port);

        ServiceConfig<MetadataService> serviceConfig = exportMockMetadataService(metadataService, port);

        DNSResolver dnsResolver = Mockito.mock(DNSResolver.class);
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.getHostnameList().add("127.0.0.1");
        Mockito.when(dnsResolver.resolve("Test.Service.")).thenReturn(resolveResult);
        dnsServiceDiscovery.setDnsResolver(dnsResolver);

        List<ServiceInstance> serviceInstances = dnsServiceDiscovery.getInstances("Test.Service.");
        assertEquals("b", serviceInstances.get(0).getMetadata("a"));

        Set<String> serviceNames = new HashSet<>();
        serviceNames.add("Test.Service.");
        ServiceInstancesChangedListener changedListener = Mockito.spy(new ServiceInstancesChangedListener(serviceNames, null));
        Mockito.doNothing().when(changedListener).onEvent(Mockito.any());

        serviceInstance.getMetadata().put("a", "c");
        dnsServiceDiscovery.update(serviceInstance);

        serviceInstances = dnsServiceDiscovery.getInstances("Test.Service.");
        assertEquals("c", serviceInstances.get(0).getMetadata("a"));

        dnsServiceDiscovery.addServiceInstancesChangedListener(changedListener);
        ArgumentCaptor<ServiceInstancesChangedEvent> argument = ArgumentCaptor.forClass(ServiceInstancesChangedEvent.class);
        Mockito.verify(changedListener, Mockito.timeout(1000)).onEvent(argument.capture());
        assertEquals("c", argument.getValue().getServiceInstances().get(0).getMetadata("a"));


        metadataService.exportInstanceMetadata(null);
        metadataService.getInstanceMetadataChangedListenerMap().clear();
        serviceConfig.unexport();

        dnsServiceDiscovery.destroy();
    }

    private ServiceConfig<MetadataService> exportMockMetadataService(MetadataService metadataService, int port) {
        ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setProtocol(new ProtocolConfig(DUBBO_PROTOCOL, port));
        serviceConfig.setRegistry(new RegistryConfig("239.255.255.255", "multicast"));
        serviceConfig.setInterface(MetadataService.class);
        serviceConfig.setRef(metadataService);
        serviceConfig.setGroup("Test.Service.");
        serviceConfig.setVersion(MetadataService.VERSION);
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("getAndListenInstanceMetadata");

        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(1);
        argumentConfig.setCallback(true);

        methodConfig.setArguments(Collections.singletonList(argumentConfig));
        serviceConfig.setMethods(Collections.singletonList(methodConfig));

        serviceConfig.export();

        return serviceConfig;
    }
}
