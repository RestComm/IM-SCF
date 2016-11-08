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
package org.restcomm.imscf.sl.diameter;

import org.restcomm.imscf.sl.config.ConfigBean;
import org.restcomm.imscf.sl.stack.SlElMappingData;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * Singleton bean of {@link SLELDiameterRouter}.
 */
@Singleton
@DependsOn(value = "ConfigBean")
public class ELDiameterRouterBean extends SLELDiameterRouter<SlElMappingData> {

    @EJB
    ConfigBean configBean;

    @PostConstruct
    protected void init() {
        super.init(configBean.getConfig());
    }

    @PreDestroy
    @Override
    protected void deinit() {
        super.deinit();
    }

}
