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

import org.restcomm.imscf.el.cap.CAPModule;
import org.restcomm.imscf.el.cap.scenarios.CapIncomingRequestScenario;
import org.restcomm.imscf.el.cap.scenarios.CapOutgoingRequestScenario;
import org.restcomm.imscf.el.tcap.call.TCAPCall;

import java.util.List;

import org.mobicents.protocols.ss7.cap.api.CAPDialog;

/**
 * Interface representing a CAP call.
 * @param <DialogType> the dialog subtype of the specific CAP service.
 */
public interface CAPCall<DialogType extends CAPDialog> extends TCAPCall {

    void setCapDialog(DialogType capDialog);

    DialogType getCapDialog();

    void setCapModule(CAPModule capModule);

    CAPModule getCapModule();

    @Override
    default long getMaxAge() {
        return getCapModule().getModuleConfiguration().getGeneralProperties().getMaxCallLengthMinutes() * 60000L;
    }

    List<CapOutgoingRequestScenario> getCapOutgoingRequestScenarios();

    List<CapIncomingRequestScenario<?>> getCapIncomingRequestScenarios();

    default void add(CapOutgoingRequestScenario scenario) {
        if (scenario != null) {
            getCapOutgoingRequestScenarios().add(scenario);
	    }
    }

    default void add(CapIncomingRequestScenario<?> scenario) {
        if (scenario != null) {
            getCapIncomingRequestScenarios().add(scenario);
	    }
    }
}
