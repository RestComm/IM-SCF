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
package org.restcomm.imscf.sl.diameter.listener;

import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for DiameterGW module listening to jdiameter stack state change events.
 */
public class SLDiameterCCAStateChangeListener implements StateChangeListener<AppSession> {

    private static final Logger LOG = LoggerFactory.getLogger(SLDiameterCCAStateChangeListener.class);

    @Override
    public void stateChanged(Enum oldState, Enum newState) {
        LOG.info("[DiameterGWServerCCAStateChangeListener] - Diameter CCA SessionFactory :: stateChanged ::  oldState[{"
                + oldState + "}], newState[{" + newState + "}]");
    }

    @Override
    public void stateChanged(AppSession source, Enum oldState, Enum newState) {
        LOG.info("[DiameterGWServerCCAStateChangeListener] - Diameter CCA SessionFactory :: stateChanged ::  oldState[{"
                + oldState + "}], newState[{" + newState + "}]");
    }

}
