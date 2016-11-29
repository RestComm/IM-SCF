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
package org.restcomm.imscf.el.sccp;

import java.util.concurrent.ConcurrentHashMap;

import org.restcomm.imscf.common.config.ImscfConfigType;
import org.restcomm.imscf.common.config.ImscfConfigType.Sccp;
import org.restcomm.imscf.el.modules.ModuleInitializationException;

import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SCCP module implementation. */
public class SccpModuleImpl implements SccpModule {

    private static final Logger LOG = LoggerFactory.getLogger(SccpModuleImpl.class);

    private String name;
    private ImscfConfigType imscfConfiguration;
    private Sccp moduleConfiguration;
    private SccpProvider sccpProvider;

    // addresses stored by alias
    private ConcurrentHashMap<String, SccpAddress> localAddresses = new ConcurrentHashMap<String, SccpAddress>();
    private ConcurrentHashMap<String, SccpAddress> remoteAddresses = new ConcurrentHashMap<String, SccpAddress>();

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ImscfConfigType getImscfConfiguration() {
        return imscfConfiguration;
    }

    @Override
    public void setImscfConfiguration(ImscfConfigType imscfConfiguration) {
        this.imscfConfiguration = imscfConfiguration;
    }

    @Override
    public Sccp getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Override
    public void setModuleConfiguration(Sccp moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    @Override
    public void setSccpProvider(SccpProvider provider) {
        this.sccpProvider = provider;
    }

    @Override
    public SccpProvider getSccpProvider() {
        return sccpProvider;
    }

    @Override
    public void initialize(ImscfConfigType configuration) throws ModuleInitializationException {
        SccpModule.super.initialize(configuration);
        LOG.debug("SCCP module {} processing shared config", getName());
    }

    @Override
    public void initialize(Sccp configuration) throws ModuleInitializationException {
        LOG.debug("SCCP module {} initializing...", getName());
        SccpModule.super.initialize(configuration);

        ParameterFactory paramFactory = getSccpProvider().getParameterFactory();
        localAddresses.clear();
        remoteAddresses.clear();

        getModuleConfiguration().getSccpLocalProfile().getLocalGtAddresses().forEach(gt -> {
            localAddresses.put(gt.getAlias(), SccpPrimitiveMapper.createSccpAddress(gt, paramFactory));
        });

        getModuleConfiguration().getSccpLocalProfile().getLocalSubSystems().forEach(ss -> {
            localAddresses.put(ss.getAlias(), SccpPrimitiveMapper.createSccpAddress(ss, paramFactory));
        });

        getModuleConfiguration().getSccpRemoteProfile().getRemoteGtAddresses().forEach(gt -> {
            remoteAddresses.put(gt.getAlias(), SccpPrimitiveMapper.createSccpAddress(gt, paramFactory, true));
        });

        getModuleConfiguration().getSccpRemoteProfile().getRemoteSubSystemPointCodeAddresses().forEach(ss -> {
            remoteAddresses.put(ss.getAlias(), SccpPrimitiveMapper.createSccpAddress(ss, paramFactory));
        });

        this.moduleConfiguration = configuration;
        LOG.debug("SCCP module {} initialized.", getName());
    }

    @Override
    public SccpAddress getLocalAddress(String alias) {
        return localAddresses.get(alias);
    }

    @Override
    public SccpAddress getRemoteAddress(String alias) {
        return remoteAddresses.get(alias);
    }

}
