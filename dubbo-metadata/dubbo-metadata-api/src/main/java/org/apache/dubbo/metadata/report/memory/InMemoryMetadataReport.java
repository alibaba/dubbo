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
package org.apache.dubbo.metadata.report.memory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MetadataReport} implementation is based on in-memory data structure.
 *
 * @see MetadataReport
 * @since 2.7.7
 */
public class InMemoryMetadataReport extends AbstractMetadataReport {

    public InMemoryMetadataReport(URL reportServerURL) {
        super(reportServerURL);
    }

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        store.put(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
        store.put(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), serviceParameterString);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
        store.put(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), url.toFullString());
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        store.remove(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        return Arrays.asList(store.getOrDefault(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), ""));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urls) {
        store.put(subscriberMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), urls);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier metadataIdentifier) {
        throw new UnsupportedOperationException("This extension does not support working as a remote metadata center.");
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier consumerMetadataIdentifier) {
        return store.get(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
    }
}
