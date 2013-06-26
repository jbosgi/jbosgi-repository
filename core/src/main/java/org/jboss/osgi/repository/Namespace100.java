/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.osgi.repository;

import java.util.HashMap;
import java.util.Map;


/**
 * Constants related to namespace
 *
 * http://www.osgi.org/xmlns/repository/v1.0.0
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-May-2012
 */
public interface Namespace100 {

    String REPOSITORY_NAMESPACE = "http://www.osgi.org/xmlns/repository/v1.0.0";

    enum Attribute {
        UNKNOWN(null),
        NAME("name"),
        NAMESPACE("namespace"),
        INCREMENT("increment"),
        VALUE("value"),
        TYPE("type"),
        ;
        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        public String getLocalName() {
            return name;
        }

        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Attribute forName(String localName) {
            final Attribute element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

        public String toString() {
            return getLocalName();
        }
    }

    enum Element {
        UNKNOWN(null),
        ATTRIBUTE("attribute"),
        CAPABILITY("capability"),
        DIRECTIVE("directive"),
        REQUIREMENT("requirement"),
        REPOSITORY("repository"),
        RESOURCE("resource"),
        ;

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        public String getLocalName() {
            return name;
        }

        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Type {
        String,
        Version,
        Long,
        Double
    }
}
