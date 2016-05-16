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
package org.restcomm.imscf.sl.config;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.SignalingLayerServerType;

import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl;
import org.mobicents.protocols.ss7.sccp.SccpStack;

/**
 * Holder object of SS7 stack objects, representing SCCP level JSs7 stack.
 *
 * @author Balogh GÃ¡bor
 *
 */
public class ImscfSigtranStack implements SigtranStack {

    protected Management sctpManagement;
    protected M3UAManagementImpl m3uaManagement;
    protected SccpStack sccpStack;
    protected final Ss7StackParameters stackParameters;
    protected final ImscfConfigType config;
    protected final String serverName;
    protected final SignalingLayerServerType server;

    public ImscfSigtranStack(Ss7StackParameters stackParameters, ImscfConfigType config, String serverName) {
        super();
        this.stackParameters = stackParameters;
        this.config = config;
        this.serverName = serverName;
        this.server = ImscfConfigUtil.getServerConfigByName(serverName, config);

    }

    protected void setSctpManagement(Management sctpManagement) {
        this.sctpManagement = sctpManagement;
    }

    protected void setM3uaManagement(M3UAManagementImpl m3uaManagement) {
        this.m3uaManagement = m3uaManagement;
    }

    protected void setSccpStack(SccpStack sccpStack) {
        this.sccpStack = sccpStack;
    }

    public Management getSctpManagement() {
        return sctpManagement;
    }

    public M3UAManagementImpl getM3uaManagement() {
        return m3uaManagement;
    }

    public SccpStack getSccpStack() {
        return sccpStack;
    }

    public Ss7StackParameters getStackParameters() {
        return stackParameters;
    }

    public ImscfConfigType getConfig() {
        return config;
    }

    public String getServerName() {
        return serverName;
    }

    public SignalingLayerServerType getServer() {
        return server;
    }

}
