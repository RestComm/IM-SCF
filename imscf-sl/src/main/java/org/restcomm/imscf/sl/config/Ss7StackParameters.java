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

import org.mobicents.protocols.api.PayloadData;

/**
 * Representing JSs7 stack parameters.
 *
 * @author Balogh GÃ¡bor
 *
 */
public class Ss7StackParameters {

    public static final int CAP_SSN = 146;
    public static final int TRANSLATION_TYPE_FOR_GTT = 0;

    private static final boolean SCTP_IS_USE_MULTI_MANAGEMENT = true;
    private static final int SCTP_CONNECT_DELAY_MILLIS = 10000;
    private static final byte[] INIT_PAYLOAD_DATA_BYTES = new byte[]{0x01, 0x00, 0x02, 0x03, 0x00, 0x00, 0x00, 0x18, 0x00, 0x06, 0x00, 0x08, 0x00, 0x00, 0x00, 0x05, 0x00, 0x12, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00};
    private static final PayloadData INIT_PAYLOAD_DATA = new PayloadData(INIT_PAYLOAD_DATA_BYTES.length, INIT_PAYLOAD_DATA_BYTES, true, false, 0, 0);

    private boolean sctpUseMultiManagement;
    private int sctpConnectDelayMillis;
    private PayloadData initPayloadData;

    public Ss7StackParameters(boolean sctpUseMultiManagement, int sctpConnectDelayMillis, PayloadData initPayloadData) {
        super();
        this.sctpUseMultiManagement = sctpUseMultiManagement;
        this.sctpConnectDelayMillis = sctpConnectDelayMillis;
        this.initPayloadData = initPayloadData;
    }

    public boolean isSctpUseMultiManagement() {
        return sctpUseMultiManagement;
    }

    public void setSctpUseMultiManagement(boolean sctpUseMultiManagement) {
        this.sctpUseMultiManagement = sctpUseMultiManagement;
    }

    public int getSctpConnectDelayMillis() {
        return sctpConnectDelayMillis;
    }

    public void setSctpConnectDelayMillis(int sctpConnectDelayMillis) {
        this.sctpConnectDelayMillis = sctpConnectDelayMillis;
    }

    public PayloadData getInitPayloadData() {
        return initPayloadData;
    }

    public void setInitPayloadData(PayloadData initPayloadData) {
        this.initPayloadData = initPayloadData;
    }

    public static Ss7StackParameters createDefaultParameters() {
        return new Ss7StackParameters(SCTP_IS_USE_MULTI_MANAGEMENT, SCTP_CONNECT_DELAY_MILLIS, INIT_PAYLOAD_DATA);
    }
}
