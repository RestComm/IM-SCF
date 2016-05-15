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
package org.restcomm.imscf.common .messages;

import com.google.gson.Gson;

import org.restcomm.imscf.common .SccpDialogId;
import org.restcomm.imscf.common .TcapDialogId;

/**
 * SCCP management message between SL and EL.
 * @author Miklos Pocsaji
 *
 */
public final class SccpManagementMessage {

    private static final ThreadLocal<Gson> GSON = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };

    /**
     * The type of the message.
     * @author Miklos Pocsaji
     *
     */
    public enum Type {
        /** A specific call needs to be deleted. The tcapDialogId and sccpDialogId attributes must be filled. */
        DeleteCall
    }

    private Type type;
    private SccpDialogId sccpDialogId;
    private TcapDialogId tcapDialogId;

    private SccpManagementMessage() {
        // Empty constructor
    }

    public static SccpManagementMessage createDeleteCallMessage(SccpDialogId sccpDialogId, TcapDialogId tcapDialogId) {
        SccpManagementMessage ret = new SccpManagementMessage();
        ret.type = Type.DeleteCall;
        ret.sccpDialogId = sccpDialogId;
        ret.tcapDialogId = tcapDialogId;
        return ret;
    }

    public static SccpManagementMessage deserialize(String data) {
        return GSON.get().fromJson(data, SccpManagementMessage.class);
    }

    public String serialize() {
        return GSON.get().toJson(this);
    }

    public Type getType() {
        return type;
    }

    public SccpDialogId getSccpDialogId() {
        return sccpDialogId;
    }

    public TcapDialogId getTcapDialogId() {
        return tcapDialogId;
    }

}
