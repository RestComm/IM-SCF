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

import org.restcomm.imscf.common.config.SipApplicationServerGroupType;
import org.restcomm.imscf.el.sip.SIPCall;
import org.restcomm.imscf.el.sip.Scenario;
import org.restcomm.imscf.el.sip.SipModule;
import org.restcomm.imscf.el.sip.failover.SipAsLoadBalancer.FailoverContext;

import java.util.List;

import javax.servlet.sip.SipServletRequest;

/** Default delegating interface. */
interface DelegatingSIPCall extends DelegatingIMSCFCall, SIPCall {

    @Override
    default SIPCall getDelegate() {
        return (SIPCall) DelegatingIMSCFCall.super.getDelegate();
    }

    @Override
    default void setSipModule(SipModule sipModule) {
        getDelegate().setSipModule(sipModule);
    }

    @Override
    default SipModule getSipModule() {
        return getDelegate().getSipModule();
    }

    @Override
    default List<Scenario> getSipScenarios() {
        return getDelegate().getSipScenarios();
    }

    @Override
    default void queueMessage(SipServletRequest msg) {
        getDelegate().queueMessage(msg);
    }

    @Override
    default List<SipApplicationServerGroupType> getAppChain() {
        return getDelegate().getAppChain();
    }

    @Override
    default FailoverContext getFailoverContext() {
        return getDelegate().getFailoverContext();
    }

    @Override
    default void setFailoverContext(FailoverContext ctx) {
        getDelegate().setFailoverContext(ctx);
    }

    @Override
    default void disableSipDialogCreation() {
        getDelegate().disableSipDialogCreation();
    }

    @Override
    default boolean isSipDialogCreationDisabled() {
        return getDelegate().isSipDialogCreationDisabled();
    }
}
