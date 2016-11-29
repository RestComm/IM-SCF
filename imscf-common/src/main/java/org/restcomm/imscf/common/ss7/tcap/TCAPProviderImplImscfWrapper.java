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
package org.restcomm.imscf.common.ss7.tcap;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.tcap.TCAPProviderImpl;
import org.mobicents.protocols.ss7.tcap.TCAPStackImpl;
import org.mobicents.protocols.ss7.tcap.api.TCListener;

/**
 * Wrapper class of TCAPProviderImplImscfWrapper.
 * Cooperating with other wrapper classes it extends the capability of the original stack implementation
 * with features like configurable message routing which allows to use more than one higher level layer stack based on
 * the same TCAP stack.
 *
 *
 * @author Balogh Gábor
 *
 */
@SuppressWarnings("serial")
public class TCAPProviderImplImscfWrapper extends TCAPProviderImpl {
    private final TCAPListenerImscfAdapter adapter;
    private ScheduledFuture<?> purgeTaskFuture;

    protected TCAPProviderImplImscfWrapper(SccpProvider sccpProvider, TCAPStackImpl stack, int ssn) {
        super(sccpProvider, stack, ssn);
        adapter = new TCAPListenerImscfAdapter(this);
        addTCListener(adapter);
    }

    @Override
    public void removeTCListener(TCListener lst) {
        adapter.removeTCListener(lst);
    }

    @Override
    public void addTCListener(TCListener lst) {
        adapter.addTCListener(lst);
    }

    void schedulePurgeTask() {
        if (_EXECUTOR instanceof ScheduledThreadPoolExecutor) {
            purgeTaskFuture = _EXECUTOR.scheduleAtFixedRate(new PurgeTask((ScheduledThreadPoolExecutor) _EXECUTOR), 1,
                    1, TimeUnit.MINUTES);
        }
    }

    void cancelPurgeTask() {
        if (purgeTaskFuture != null) {
            purgeTaskFuture.cancel(false);
            purgeTaskFuture = null;
        }
    }

    /**
     * Task which calls purge() on the given ScheduledThreadPoolExecutor.
     * @author Miklos Pocsaji
     *
     */
    private static class PurgeTask implements Runnable {
        private ScheduledThreadPoolExecutor executor;

        public PurgeTask(ScheduledThreadPoolExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void run() {
            executor.purge();
        }
    }

}
