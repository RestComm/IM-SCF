/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011­2016, Telestax Inc and individual contributors
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

/**
 * Base class for SS7 stack listeners.
 */
public class ImscfStackListener {
    protected CallStore callStore;
    protected CallFactoryBean callFactory;

    protected ImscfStackListener() {
        // protected constructor for subclasses
    }

    public void setCallFactory(CallFactoryBean callFactory) {
        this.callFactory = callFactory;
    }

    public void setCallStore(CallStore callStore) {
        this.callStore = callStore;
    }

    public CallFactoryBean getCallFactory() {
        return callFactory;
    }

    public CallStore getCallStore() {
        return callStore;
    }
}
