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
package org.apache.dubbo.governance.support.nop;

import org.apache.dubbo.governance.AbstractDynamicConfiguration;
import org.apache.dubbo.governance.ConfigurationListener;

/**
 *
 */
public class NopDynamicConfiguration extends AbstractDynamicConfiguration {

    @Override
    public void init() {

    }

    @Override
    protected String getInternalProperty(String key, String group, long timeout) {
        return null;
    }

    @Override
    protected void addTargetListener(String key, Object o) {

    }

    @Override
    protected Object createTargetConfigListener(String key, ConfigurationListener listener) {
        return null;
    }
}
