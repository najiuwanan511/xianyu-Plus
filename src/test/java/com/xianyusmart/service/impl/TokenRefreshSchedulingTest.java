package com.xianyusmart.service.impl;

import com.xianyusmart.config.WebSocketConfig;
import com.xianyusmart.mapper.XianyuAccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TokenRefreshSchedulingTest {

    @Test
    void proactiveCredentialTasksDoNothingByDefault() {
        TokenRefreshServiceImpl service = new TokenRefreshServiceImpl();
        WebSocketConfig config = new WebSocketConfig();
        XianyuAccountMapper accountMapper = mock(XianyuAccountMapper.class);
        ReflectionTestUtils.setField(service, "webSocketConfig", config);
        ReflectionTestUtils.setField(service, "accountMapper", accountMapper);

        service.initRefreshSchedules();
        service.scheduledCookieKeepAlive();
        service.scheduledRefreshWebSocketToken();

        assertEquals(Long.MAX_VALUE,
                (long) ReflectionTestUtils.getField(service, "nextCookieKeepAliveTime"));
        verifyNoInteractions(accountMapper);
    }
}
