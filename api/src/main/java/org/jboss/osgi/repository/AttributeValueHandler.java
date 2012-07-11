/*
 * #%L
 * JBossOSGi Repository: API
 * %%
 * Copyright (C) 2011 - 2012 JBoss by Red Hat
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

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;

import org.jboss.osgi.repository.Namespace100.Type;
import org.osgi.framework.Version;

/**
 * A handler for attribute values.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Jul-2012
 */
public final class AttributeValueHandler {

    /**
     * Read attribute values according to
     * 132.5.6 Attribute Element
     */
    public static AttributeValue readAttributeValue(String typespec, String valstr) {
        boolean listType = false;
        if (typespec != null && typespec.startsWith("List<") && typespec.endsWith(">")) {
            typespec = typespec.substring(5, typespec.length() - 1);
            listType = true;
        }
        Type type = typespec != null ? Type.valueOf(typespec) : Type.String;

        // Whitespace around the list and around commas must be trimmed
        valstr = valstr.trim();

        Object value;
        switch (type) {
            case String:
                if (listType) {
                    List<String> list = new ArrayList<String>();
                    for (String val : split(valstr)) {
                        list.add(val.trim());
                    }
                    value = list;
                } else {
                    value = valstr;
                }
                break;
            case Version:
                if (listType) {
                    List<Version> list = new ArrayList<Version>();
                    for (String val : split(valstr)) {
                        list.add(Version.parseVersion(val.trim()));
                    }
                    value = list;
                } else {
                    value = Version.parseVersion(valstr);
                }
                break;
            case Long:
                if (listType) {
                    List<Long> list = new ArrayList<Long>();
                    for (String val : split(valstr)) {
                        list.add(Long.parseLong(val.trim()));
                    }
                    value = list;
                } else {
                    value = Long.parseLong(valstr);
                }
                break;
            case Double:
                if (listType) {
                    List<Double> list = new ArrayList<Double>();
                    for (String val : split(valstr)) {
                        list.add(Double.parseDouble(val.trim()));
                    }
                    value = list;
                } else {
                    value = Double.parseDouble(valstr);
                }
                break;
            default:
                value = valstr;
                break;
        }
        return new AttributeValue(type, value);
    }

    private static List<String> split(String valstr) {
        boolean escape = false;
        StringBuffer tok = new StringBuffer();
        List<String> result = new ArrayList<String>();

        for (int i = 0; i < valstr.length(); i++) {
            char ch = valstr.charAt(i);
            if (ch == '\\' && !escape) {
                escape = true;
                continue;
            }
            if (escape && ch != '\\' && ch != ',') {
                tok.append('\\');
                escape = false;
            }
            if (ch == ',' && !escape) {
                result.add(tok.toString());
                tok = new StringBuffer();
                escape = false;
                continue;
            }
            tok.append(ch);
            escape = false;
        }
        if (tok.length() > 0) {
            result.add(tok.toString());
        }
        return result;
    }

    public static class AttributeValue {
        private final Type type;
        private final Object value;
        private final boolean listType;

        public static AttributeValue parse(String external) {
            String typespec = external.substring(external.indexOf("type=") + 5, external.indexOf(','));
            String valuestr = external.substring(external.indexOf("value=") + 6, external.length() - 1);
            return AttributeValueHandler.readAttributeValue(typespec, valuestr);
        }

        public static AttributeValue create(Object value) {
            Class<?> valueType = value.getClass();
            boolean listType = List.class.isAssignableFrom(valueType);
            if (listType) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty()) {
                    // Use the type of the first element in the list
                    valueType = list.get(0).getClass();
                } else {
                    // For an empty list it is not possible to infer it's component type
                    valueType = String.class;
                }
            }
            Type type = Type.valueOf(valueType.getSimpleName());
            return new AttributeValue(type, value);
        }

        private AttributeValue(Type type, Object value) {
            if (type == null)
                throw MESSAGES.illegalArgumentNull("type");
            if (value == null)
                throw MESSAGES.illegalArgumentNull("value");
            this.type = type;
            this.value = value;
            Class<? extends Object> valueClass = value.getClass();
            this.listType = List.class.isAssignableFrom(valueClass);
        }

        public Type getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }

        public String getValueString() {
            StringBuffer result = new StringBuffer();
            if (listType) {
                for (Object val : (List<?>) value) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append(escape(val));
                }
            } else {
                result.append(escape(value));
            }
            return result.toString();
        }

        private String escape(Object val) {
            String valstr = val.toString();
            if (type != Type.String)
                return valstr;
            
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < valstr.length(); i++) {
                char ch = valstr.charAt(i);
                if (ch == '\\' || ch == ',') {
                    result.append("\\" + ch);
                } else {
                    result.append(ch);
                }
            }
            return result.toString();
        }

        public boolean isListType() {
            return listType;
        }

        public String toExternalForm() {
            String typespec = listType ? "List<" + type + ">" : "" + type;
            String valstr = value.toString();
            if (listType) {
                valstr = valstr.substring(1, valstr.length() -1);
            }
            return "[type=" + typespec + ", value=" + valstr + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AttributeValue other = (AttributeValue) obj;
            if (type != other.type)
                return false;
            return value.equals(other.value);
        }

        @Override
        public String toString() {
            return toExternalForm();
        }
    }
}
