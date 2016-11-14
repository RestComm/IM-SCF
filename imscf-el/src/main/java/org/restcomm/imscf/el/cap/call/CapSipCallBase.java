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
package org.restcomm.imscf.el.cap.call;

import org.restcomm.imscf.el.call.impl.TCAPSIPCallBase;
import org.restcomm.imscf.el.cap.scenarios.CapIncomingRequestScenario;
import org.restcomm.imscf.el.cap.scenarios.CapOutgoingRequestScenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Extension of TCAP-SIP call. */
public abstract class CapSipCallBase extends TCAPSIPCallBase {

    protected List<CapOutgoingRequestScenario> capOutgoingScenarios = Collections.synchronizedList(new ArrayList<>());
    protected List<CapIncomingRequestScenario<?>> capIncomingScenarios = Collections.synchronizedList(new ArrayList<>());

    public List<CapOutgoingRequestScenario> getCapOutgoingRequestScenarios() {
        return capOutgoingScenarios;
    }

    public List<CapIncomingRequestScenario<?>> getCapIncomingRequestScenarios() {
        return capIncomingScenarios;
    }
}
