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
package org.apache.cxf.blueprint.jaxws.handler;

import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.blueprint.jaxws.CxfEmbeddedJettyBluePrintBean;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Resource;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;


public class CxfJaxwsNamespaceHandler implements NamespaceHandler {

    private static final String JAXWS_NS = "http://cxf.apache.org/jaxws";
    private static final String ENDPOINT = "jaxws:endpoint";

    @Resource
    Bundle bundle;

    BundleContext ctx;

    @Override
    public URL getSchemaLocation(String s) {
        System.out.println("Asked for schema for " + s);
        return getClass().getClassLoader().getResource("jaxws.xsd");
    }

    @Override
    public Set<Class> getManagedClasses() {
        return new HashSet<Class>(Arrays.asList(
                Server.class
        ));
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        System.out.println("Got an element : " + element.getNodeName());

        // Find the id, generate one if needed
        String contextId = element.getAttribute("id");
        if (StringUtils.isEmpty(contextId)) {
            contextId = "cxfJaxWsContext";
            element.setAttribute("id", contextId);
        }

        NamedNodeMap atts = element.getAttributes();
        String bus = element.getAttribute("bus");
        Bus b = null;
        if (ENDPOINT.equals(element.getNodeName())) {
            boolean isAbstract = false;

            if (StringUtils.isEmpty(bus)) {
                // Create a new bus or use the bus provided
                System.out.println("Creating a simple bus");
                bus = "defaultBus";
                b = rideTheShortBus();

            } else {

                BusFactory bf = BusFactory.newInstance(bus);
                b = bf.createBus();
            }

            System.out.print("Checking implementors...");
            Set<String> components = context.getComponentDefinitionRegistry().getComponentDefinitionNames();

            for (Object component : components.toArray()) {

                System.out.println("Component " + component);

                System.out.println(component.getClass());
            }

            CxfEmbeddedJettyBluePrintBean bean = new CxfEmbeddedJettyBluePrintBean();

            MutableBeanMetadata md = context.createMetadata(MutableBeanMetadata.class);
            // Class that will be instantiated
            md.setRuntimeClass(CxfEmbeddedJettyBluePrintBean.class);

            /**
             Object implementationClass;
             String interfaceClass;
             String endpointUrl;
             String endpointName;
             */

            //  md.addProperty("implementationClass", createValue(context, childElement.getAttribute("className")));

            md.addProperty("implementationClass", createRef(context, element.getAttribute("implementor")));
            md.addProperty("implementationInterface", createRef(context, element.getAttribute("implementor")));


            //if (childElement.getAttribute("flags") != null) {
            //  md.addProperty("flags", createValue(context, childElement.getAttribute("flags")));
            //}


            JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
            //svrFactory.setServiceClass(NotificationProducer.class);
            svrFactory.setAddress(element.getAttribute("address").toString());
            // svrFactory.setServiceBean(subs);
            svrFactory.getInInterceptors().add(new LoggingInInterceptor());
            svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
            Server srv = svrFactory.create();

            System.out.println("Will publish to " + element.getAttribute("address"));


            MutablePassThroughMetadata factory = context.createMetadata(MutablePassThroughMetadata.class);
            factory.setId(".jaxwsServer.passThrough." + contextId);
            factory.setActivation(1);
            factory.setObject(new PassThroughCallable<Object>(srv));


            /*MutableBeanMetadata factory2 = context.createMetadata(MutableBeanMetadata.class);
                        factory2.setId(".cxfBlueprint.factory." + contextId);
                        factory2.setFactoryComponent(factory);
                        factory2.setFactoryMethod("call");
                        factory2.setInitMethod("init");
                        factory2.setDestroyMethod("destroy");


            */

            MutableBeanMetadata ctx = context.createMetadata(MutableBeanMetadata.class);
            ctx.setId(contextId);
            ctx.setFactoryComponent(factory);
            // ctx.setFactoryMethod("getContext");
            ctx.setInitMethod("start");
            ctx.setDestroyMethod("stop");
            return factory;


        }


        return null;

    }

    public void parseSettings(Element element, EndpointImpl endpoint) {

        NamedNodeMap atts = element.getAttributes();
        /**
         for (int i = 0; i < atts.getLength(); i++) {
         Attr node = (Attr) atts.item(i);
         String val = node.getValue();
         String pre = node.getPrefix();
         String name = node.getLocalName();

         if ("createdFromAPI".equals(name)) {
         bean.setAbstract(true);
         isAbstract = true;
         } else if (isAttribute(pre, name) && !"publish".equals(name) && !"bus".equals(name)) {
         if ("endpointName".equals(name) || "serviceName".equals(name)) {
         QName q = parseQName(element, val);
         bean.addPropertyValue(name, q);
         } else if ("depends-on".equals(name)) {
         bean.addDependsOn(val);
         } else if (IMPLEMENTOR.equals(name)) {
         loadImplementor(bean, val);
         } else if (!"name".equals(name)) {
         mapToProperty(bean, name, val);
         }
         } else if ("abstract".equals(name)) {
         bean.setAbstract(true);
         isAbstract = true;
         }
         }

         Element elem = DOMUtils.getFirstElement(element);
         while (elem != null) {
         String name = elem.getLocalName();
         if ("properties".equals(name)) {
         Map map = ctx.getDelegate().parseMapElement(elem, bean.getBeanDefinition());
         bean.addPropertyValue("properties", map);
         } else if ("binding".equals(name)) {
         setFirstChildAsProperty(elem, ctx, bean, "bindingConfig");
         } else if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
         || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
         || "features".equals(name) || "schemaLocations".equals(name)
         || "handlers".equals(name)) {
         List list = ctx.getDelegate().parseListElement(elem, bean.getBeanDefinition());
         bean.addPropertyValue(name, list);
         } else if (IMPLEMENTOR.equals(name)) {
         ctx.getDelegate()
         .parseConstructorArgElement(elem, bean.getBeanDefinition());
         } else {
         setFirstChildAsProperty(elem, ctx, bean, name);
         }
         elem = DOMUtils.getNextElement(elem);
         }
         if (!isAbstract) {
         bean.setInitMethodName("publish");
         bean.setDestroyMethodName("stop");
         }
         // We don't want to delay the registration of our Server
         bean.setLazyInit(false);
         */

    }

    private RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
        r.setComponentId(value);
        return r;
    }

    private ValueMetadata createValue(ParserContext context, String value) {
        MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
        v.setStringValue(value);
        return v;
    }

    @Override
    public ComponentMetadata decorate
            (Node
                    node, ComponentMetadata
                    componentMetadata, ParserContext
                    parserContext) {


        return null;  //TODO
    }

    protected Bus rideTheShortBus() {

        BusFactory busFactory = new CXFBusFactory();
        return busFactory.createBus();

    }

    public static class PassThroughCallable<T> implements Callable<T> {

        private T value;

        public PassThroughCallable(T value) {
            this.value = value;
        }

        public T call() throws Exception {
            return value;
        }
    }


}
