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
package org.restcomm.imscf.el.cap.converter;

import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.ImscfCallLifeCycleListener;
import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.call.CapSipCsCall;
import org.restcomm.imscf.el.cap.call.CapSipSmsCall;
import org.restcomm.imscf.el.sip.SipModule;

/**
 * CAP &lt;-&gt; SIP converter module.
 */
public interface CapSipConverter extends CAPModule, SipModule, ImscfCallLifeCycleListener {

    @Override
    default void imscfCallStateChanged(IMSCFCall call) {
        if (call instanceof CapSipCsCall)
            imscfCallStateChanged((CapSipCsCall) call);
        else if (call instanceof CapSipSmsCall)
            imscfCallStateChanged((CapSipSmsCall) call);
        else
            throw new IllegalArgumentException("Unsupported call type " + call);
    }

    void imscfCallStateChanged(CapSipCsCall call);

    void imscfCallStateChanged(CapSipSmsCall call);
}
