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
package org.restcomm.imscf.el.call.impl;

import org.restcomm.imscf.el.sip.adapters.SipApplicationSessionAdapter;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.sip.SipApplicationSession;

import org.mobicents.protocols.ss7.tcap.api.tc.dialog.LockAction;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LockAction implementation used in TCAP dialogs to lock on the external appsession belonging to the call. */
public class AppSessionLockAction implements LockAction {

    private static final Logger LOG = LoggerFactory.getLogger(AppSessionLockAction.class);
    private SipApplicationSession sas;
    // tolerate reentrant locking
    private ReentrantLock lock = new ReentrantLock(false); // no fairness required

    public AppSessionLockAction(SipApplicationSession sas) {
        this.sas = Objects.requireNonNull(SipApplicationSessionAdapter.unwrap(sas));
    }

    @Override
    public void doPostUnlockAction() {
        LOG.trace("Trying to release appsession {}", sas.getId());
        assert lock.isHeldByCurrentThread();
        if (lock.getHoldCount() == 1) { // release SAS on last unlock
            ((MobicentsSipApplicationSession) sas).release();
        }

        LOG.trace("appsession lock count decreased to {}", lock.getHoldCount() - 1); // after logging...
        lock.unlock();
    }

    @Override
    public void doPreLockAction() {
        LOG.trace("Trying to acquire appsession {}", sas.getId());
        lock.lock(); // blocks while held by a different thread
        if (lock.getHoldCount() == 1) { // only acquire semaphore on first entry
            ((MobicentsSipApplicationSession) sas).acquire();
        }
        LOG.trace("appsession lock count increased to {}", lock.getHoldCount());
    }

    @Override
    public void doPostLockAction() {
        // NOOP
    }

    @Override
    public void doPreUnlockAction() {
        // NOOP
    }

}
