package com.xianyusmart.service.impl;

import com.google.gson.JsonObject;
import com.xianyusmart.controller.dto.QRLoginSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QRLoginServiceImplTest {

    @Test
    void mergesPassportCookiesAcrossQrRequests() {
        QRLoginSession session = new QRLoginSession("session-1");

        QRLoginServiceImpl.mergeSetCookieHeaders(session, List.of(
                "loginKey=abc; Path=/; HttpOnly",
                "unb=22129504; Domain=.goofish.com; Path=/"));

        assertEquals("abc", session.getCookies().get("loginKey"));
        assertEquals("22129504", session.getUnb());
    }

    @Test
    void acceptsCurrentAndLegacyScannedStatusNames() {
        assertTrue(QRLoginServiceImpl.isScannedStatus("SCANED"));
        assertTrue(QRLoginServiceImpl.isScannedStatus("SCANNED"));
    }

    @Test
    void readsStatusFromCompatibleResponseFields() {
        JsonObject data = new JsonObject();
        data.addProperty("status", "confirmed");

        assertEquals("CONFIRMED", QRLoginServiceImpl.readQRCodeStatus(data));
    }

    @Test
    void treatsCancellationOnlyAsAnExplicitTerminalStatus() {
        assertTrue(QRLoginServiceImpl.isCancelledStatus("CANCELLED"));
        assertTrue(!QRLoginServiceImpl.isCancelledStatus("PROCESSING"));
    }
}
