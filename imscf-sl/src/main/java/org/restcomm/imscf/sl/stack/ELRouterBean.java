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
package org.restcomm.imscf.sl.stack;

import org.restcomm.imscf.sl.config.ConfigBean;
import org.restcomm.imscf.common.SLELRouter;
import org.restcomm.imscf.common.SccpDialogId;
import org.restcomm.imscf.common.TcapDialogId;
import org.restcomm.imscf.common.lwcomm.config.Route;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * Singleton bean of {@link SLELRouter}.
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
@DependsOn(value = "ConfigBean")
public class ELRouterBean extends SLELRouter<SlElMappingData> {

    @EJB
    ConfigBean configBean;

    private boolean initialized;

    @PostConstruct
    private void init() {
        if (configBean.isSigtranStackNeeded()) {
            super.init(configBean.getConfig());
            initialized = true;
        }
    }

    @PreDestroy
    @Override
    protected void deinit() {
        if (initialized) {
            super.deinit();
            initialized = false;
        }
    }

    @Override
    public String getDirectRouteNameTo(String otherNode) {
        if (!initialized)
            throw new IllegalStateException("ELRouterBean not initialized!");
        return super.getDirectRouteNameTo(otherNode);
    }

    @Override
    public SlElMappingData getMappingData(SccpDialogId sdid, TcapDialogId tdid) {
        if (!initialized)
            throw new IllegalStateException("ELRouterBean not initialized!");
        return super.getMappingData(sdid, tdid);
    }

    @Override
    public Route getRouteToAnyNode() {
        if (!initialized)
            throw new IllegalStateException("ELRouterBean not initialized!");
        return super.getRouteToAnyNode();
    }

    @Override
    public SlElMappingData setMappingData(SccpDialogId sdid, TcapDialogId tdid, SlElMappingData data) {
        if (!initialized)
            throw new IllegalStateException("ELRouterBean not initialized!");
        return super.setMappingData(sdid, tdid, data);
    }

}
