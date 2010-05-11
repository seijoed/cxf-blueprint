/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cxf.blueprint.jaxws;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

public class CxfEmbeddedJettyBluePrintBean {

    Server srv;

    Object implementationClass;
    String serviceClass;
    String endpointUrl;
    String endpointName;

    public CxfEmbeddedJettyBluePrintBean() {
        //start();
    }

    public void start() {
        // Setup the system properties to use the CXFBusFactory not the SpringBusFactory

        String busFactory = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");

        if (implementationClass == null || serviceClass == null || endpointUrl == null || endpointName == null) {
            throw new RuntimeException("Cannot instantiate a server with null passed as configuration");
        }

        try {

            Object impl;
            // This is the implementation class with its beans.
            // If we hit this, we have a very basic implementation.
            if (implementationClass == null) {
                impl = implementationClass.getClass().newInstance();
            } else {
                impl = implementationClass;
            }

            BusFactory bf = CXFBusFactory.newInstance();
            Bus b = bf.createBus();

            JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
            svrFactory.setServiceClass(getClass().getClassLoader().loadClass(serviceClass));
            svrFactory.setBus(b);
            // svrFactory.getServiceFactory().setWrapped(true);
            svrFactory.setAddress(endpointUrl);
            //svrFactory.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
            //svrFactory.getServiceFactory().setInvoker(new BeanInvoker(impl));
            svrFactory.setServiceBean(impl);

            // Make this come via configs
            svrFactory.getInInterceptors().add(new LoggingInInterceptor());
            svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());

            srv = svrFactory.create();


        } catch (Exception e) {
            throw new RuntimeException("Failed to start server " + e);
        }

    }

    public void destroy() {
        srv.stop();
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public Object getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(Object implementationClass) {
        this.implementationClass = implementationClass;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Server getSrv() {
        return srv;
    }

}
