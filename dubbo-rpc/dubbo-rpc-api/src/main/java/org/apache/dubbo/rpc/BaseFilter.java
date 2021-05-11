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
package org.apache.dubbo.rpc;

public interface BaseFilter {
    /**
     * Make sure call invoker.invoke() in your implementation.
     */
    default Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (this instanceof BaseFilter.Request) {
            InvocationWrapper invocationWrapper = new InvocationWrapper(invocation);
            try {
                Result result = ((Request) this).onBefore(invoker, invocationWrapper);
                if (result == null) {
                    result = invoker.invoke(invocationWrapper.getInvocation());
                }
                result = ((Request) this).onAfter(result, invoker, invocationWrapper);
                return result;
            } finally {
                ((Request) this).onFinish(invoker, invocationWrapper);
            }
        }
        throw new UnsupportedOperationException();
    }

    interface Request {
        Result onBefore(Invoker<?> invoker, InvocationWrapper invocationWrapper) throws RpcException;

        default Result onAfter(Result appResponse, Invoker<?> invoker, InvocationWrapper invocationWrapper) throws RpcException {
            return appResponse;
        }

        default void onFinish(Invoker<?> invoker, InvocationWrapper invocationWrapper) throws RpcException {

        }
    }

    interface Listener {

        void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation);

        void onError(Throwable t, Invoker<?> invoker, Invocation invocation);
    }
}
