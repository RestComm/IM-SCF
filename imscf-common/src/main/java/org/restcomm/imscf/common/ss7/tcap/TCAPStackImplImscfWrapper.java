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
package org.restcomm.imscf.common.ss7.tcap;

import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.tcap.TCAPCounterProviderImpl;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;

/**
 * Wrapper class of TCAPStackImpl.
 * Cooperating with other wrapper classes it extends the capability of the original stack implementation
 * with features like configurable message routing which allows to use more than one higher level layer stack based on
 * the same TCAP stack.
 *
 *
 * @author Balogh GÃ¡bor
 *
 */
public class TCAPStackImplImscfWrapper extends TCAPStackImpl {

    public TCAPStackImplImscfWrapper(String name, SccpProvider sccpProvider, int ssn) {
        super(name);
        this.tcapProvider = new TCAPProviderImplImscfWrapper(sccpProvider, this, ssn);
        this.tcapCounterProvider = new TCAPCounterProviderImpl(this.tcapProvider);
    }

    @Override
    public void start() throws Exception {
        super.start();
        if (tcapProvider instanceof TCAPProviderImplImscfWrapper) {
            ((TCAPProviderImplImscfWrapper) tcapProvider).schedulePurgeTask();
        }
    }

    @Override
    public void stop() {
        if (tcapProvider instanceof TCAPProviderImplImscfWrapper) {
            ((TCAPProviderImplImscfWrapper) tcapProvider).cancelPurgeTask();
        }
        super.stop();
    }
}
