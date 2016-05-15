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
package org.restcomm.imscf.sl.diameter.impl;

import org.jdiameter.api.Configuration;
import org.jdiameter.api.InternalException;
import org.jdiameter.client.api.fsm.IContext;
import org.jdiameter.client.api.fsm.IFsmFactory;
import org.jdiameter.common.api.concurrent.IConcurrentFactory;
import org.jdiameter.common.api.statistic.IStatisticManager;
import org.jdiameter.server.impl.fsm.FsmFactoryImpl;

/**
 * Modified FsmFactoryImpl to instantiate the modified server FSM implementation.
 *
 */
public class ImscfServerFsmFactoryImpl extends FsmFactoryImpl implements IFsmFactory {

    public ImscfServerFsmFactoryImpl(IStatisticManager statisticFactory) {
        super(statisticFactory);
    }

    @Override
    public org.jdiameter.server.api.IStateMachine createInstanceFsm(IContext context,
            IConcurrentFactory concurrentFactory, Configuration config) throws InternalException {
        return new ImscfServerPeerFSMImpl(context, concurrentFactory, config, statisticFactory);
    }

}
