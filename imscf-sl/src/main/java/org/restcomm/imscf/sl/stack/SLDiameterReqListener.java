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
package org.restcomm.imscf.sl.stack;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.common.impl.app.cca.CCASessionFactoryImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;

/**
 * Class for diameter request listener (used to process network requests).
 */
public class SLDiameterReqListener implements NetworkReqListener {

    private CCASessionFactoryImpl factory;
    private ApplicationId aid;

    public SLDiameterReqListener(CCASessionFactoryImpl factory, ApplicationId aid) {
        this.factory = factory;
        this.aid = aid;
    }

    @Override
    public Answer processRequest(Request req) {
        ServerCCASessionImpl newSession = (ServerCCASessionImpl) factory.getNewSession(req.getSessionId(),
                ServerCCASession.class, aid, new Object[] {});
        newSession.processRequest(req);
        return null;
    }
}
