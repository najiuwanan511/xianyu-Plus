package com.xianyusmart.service;

import com.xianyusmart.entity.XianyuAccount;
import com.xianyusmart.mapper.XianyuAccountMapper;
import com.xianyusmart.service.impl.CredentialUpdateCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutomaticConnectionRepairSchedulerTest {

    private final XianyuAccountMapper accountMapper = mock(XianyuAccountMapper.class);
    private final TokenRefreshService tokenRefreshService = mock(TokenRefreshService.class);
    private final WebSocketTokenService webSocketTokenService = mock(WebSocketTokenService.class);
    private final WebSocketService webSocketService = mock(WebSocketService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private AutomaticConnectionRepairScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AutomaticConnectionRepairScheduler(
                accountMapper,
                tokenRefreshService,
                webSocketTokenService,
                webSocketService,
                operationLogService,
                new CredentialUpdateCoordinator(),
                Runnable::run);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "minHours", 5);
        ReflectionTestUtils.setField(scheduler, "maxHours", 8);
        ReflectionTestUtils.setField(scheduler, "accountSpacingMinutes", 60);
    }

    @Test
    void generatesRepairDelayBetweenFiveAndEightHours() {
        for (int index = 0; index < 100; index++) {
            long delay = scheduler.randomRepairDelayMillis();
            assertTrue(delay >= TimeUnit.HOURS.toMillis(5));
            assertTrue(delay <= TimeUnit.HOURS.toMillis(8));
        }
    }

    @Test
    void repairsDueAccountsAtLeastOneHourApart() {
        XianyuAccount first = account(1L);
        XianyuAccount second = account(2L);
        when(accountMapper.selectList(any())).thenReturn(List.of(first, second));
        when(accountMapper.selectById(1L)).thenReturn(first);
        when(accountMapper.selectById(2L)).thenReturn(second);
        when(tokenRefreshService.refreshMh5tkToken(any())).thenReturn(true);
        when(tokenRefreshService.refreshWebSocketToken(any())).thenReturn(true);
        when(webSocketService.isConnected(any())).thenReturn(true);
        when(webSocketService.startWebSocket(any())).thenReturn(true);

        long now = System.currentTimeMillis();
        repairTimes().put(1L, now - 2);
        repairTimes().put(2L, now - 1);

        scheduler.runDueRepairsAt(now);
        verify(tokenRefreshService).refreshWebSocketToken(1L);
        verify(webSocketService).stopWebSocket(1L);
        verify(webSocketService).startWebSocket(1L);

        scheduler.runDueRepairsAt(now + TimeUnit.MINUTES.toMillis(59));
        verify(tokenRefreshService, never()).refreshWebSocketToken(2L);

        scheduler.runDueRepairsAt(now + TimeUnit.HOURS.toMillis(1) + TimeUnit.SECONDS.toMillis(1));
        verify(tokenRefreshService).refreshWebSocketToken(2L);
        verify(webSocketService).startWebSocket(2L);
        verify(tokenRefreshService, times(2)).refreshWebSocketToken(any());
    }

    @Test
    void skipsRepairWhileAccountWaitsForVerification() {
        XianyuAccount account = account(7L);
        when(accountMapper.selectList(any())).thenReturn(List.of(account));
        when(webSocketTokenService.isCaptchaPending(7L)).thenReturn(true);
        long now = System.currentTimeMillis();
        repairTimes().put(7L, now - 1);

        scheduler.runDueRepairsAt(now);

        verify(tokenRefreshService, never()).refreshMh5tkToken(7L);
        verify(tokenRefreshService, never()).refreshWebSocketToken(7L);
        assertTrue(repairTimes().get(7L) >= now + TimeUnit.HOURS.toMillis(1));
    }

    @Test
    void schedulesNextRunWhenRepairThrows() {
        XianyuAccount account = account(9L);
        when(accountMapper.selectList(any())).thenReturn(List.of(account));
        when(accountMapper.selectById(9L)).thenReturn(account);
        doThrow(new IllegalStateException("temporary failure"))
                .when(tokenRefreshService).refreshMh5tkToken(9L);
        long now = System.currentTimeMillis();
        repairTimes().put(9L, now - 1);

        scheduler.runDueRepairsAt(now);

        assertTrue(repairTimes().get(9L) >= now + TimeUnit.HOURS.toMillis(5));
        verify(operationLogService).log(
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.contains("自动完整修复异常"),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> repairTimes() {
        return (Map<Long, Long>) ReflectionTestUtils.getField(scheduler, "nextRepairTimes");
    }

    private static XianyuAccount account(long id) {
        XianyuAccount account = new XianyuAccount();
        account.setId(id);
        account.setStatus(1);
        return account;
    }
}
