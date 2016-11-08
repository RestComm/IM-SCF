/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.imscf.el.stack;

import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.config.ConfigBean;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Call context for thread local usage. This is used to bypass stack calls
 * where it is otherwise impossible to pass call information, e.g. from
 * LwComm to CAP listener. It is also used to propagate servlet/bean context objects.
 */
public final class CallContext {

    public static final String IMSCFCALLID = "imscfcallid";
    public static final String CALLSTORE = "callstore";
    public static final String CALLFACTORY = "callfactory";
    public static final String CONFIG = "config";

    private static final Logger LOG = LoggerFactory.getLogger(CallContext.class);

    private static final ThreadLocal<CallContext> ACTIVE = new ThreadLocal<CallContext>() {
        @Override
        protected CallContext initialValue() {
            return new CallContext();
        }
    };

    private Map<String, Object> data = new HashMap<String, Object>();

    private Map<String, Object> getData() {
        return data;
    }

    private CallContext() {
    }

    private static Map<String, Object> data() {
        return ACTIVE.get().getData();
    }

    public static CallStore getCallStore() {
        return (CallStore) get(CALLSTORE);
    }

    public static CallFactoryBean getCallFactory() {
        return (CallFactoryBean) get(CALLFACTORY);
    }

    public static ConfigBean getConfigBean() {
        return (ConfigBean) get(CONFIG);
    }

    public static Object get(String key) {
        Object ret = data().get(key);
        if (ret == null) {
            LOG.trace("key {} not found", key);
        } else {
            LOG.trace("key {} found: {}", key, ret.getClass());
        }
        return ret;
    }

    public static Object put(String key, Object value) {
        Object ret = data().put(key, value);
        LOG.trace("CallContext put {}, result: {}", key, data());
        return ret;
    }

    public static void remove(String key) {
        data().remove(key);
        LOG.trace("CallContext remove {}, result: {}", key, data());
    }

    public static void clear() {
        data().clear();
        print();
    }

    public static void print() {
        LOG.trace("CallContext: {}", data());
    }

    public static ContextLayer with(Object... params) {
        return new ContextLayerImpl(params);
    }

    /** Dummy interface to be used with {@link CallContext#with(Object...)}.*/
    public interface ContextLayer extends AutoCloseable {
        @Override
        void close(); // removed exception from signature
    }
}

/** Helper class for setting call context objects. If this is the first time
 *  in the call stack that a value is set, it is automatically removed at context release. */
class ContextLayerImpl implements CallContext.ContextLayer {
    Collection<String> firstTimeSet;

    public ContextLayerImpl(Object[] params) {
        firstTimeSet = Arrays.stream(params).map(o -> {
            String name = getName(o);
            return null == CallContext.put(name, o) ? name : null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void close() {
        firstTimeSet.forEach(CallContext::remove);
    }

    private static String getName(Object o) {
        if (o instanceof CallStore)
            return CallContext.CALLSTORE;
        else if (o instanceof CallFactoryBean)
            return CallContext.CALLFACTORY;
        else if (o instanceof ConfigBean)
            return CallContext.CONFIG;
        else if (o instanceof String)
            return CallContext.IMSCFCALLID;
        else
            throw new IllegalArgumentException("Unrecognized: " + o.getClass());
    }
}
