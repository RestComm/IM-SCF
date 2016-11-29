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
package org.restcomm.imscf.common.config.lib;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ImscfConfigType.Sccp;
import org.restcomm.imscf.common.config.SccpLocalProfileType;

import java.util.Optional;

/** Checker utility for the IMSCF config. Can be used in the IMSCF, as well as in external tools. */
public final class ImscfConfigChecker {
    private ImscfConfigType config;

    public ImscfConfigChecker(ImscfConfigType config) {
        this.config = config;
    }

    public void checkConfiguration() throws IllegalStateException {
        // SIGTRAN config checks
        if (isSigtranStackNeeded()) {
            // 1. if there is a CAP or MAP module, there must be a local GT or SSN configured as well
            SccpLocalProfileType lp = Optional.ofNullable(config.getSccp()).map(Sccp::getSccpLocalProfile).orElse(null);
            if (lp == null || (lp.getLocalGtAddresses().size() == 0 && lp.getLocalSubSystems().size() == 0))
                throw new IllegalStateException("SCCP local GT/SSN must be configured if CAP/MAP modules are used.");
        }

    }

    public boolean isSigtranStackNeeded() {
        return !config.getCapModules().isEmpty() || !config.getMapModules().isEmpty();
    }

}
