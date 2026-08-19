package com.xianyusmart.service.delivery;

import com.xianyusmart.service.AccountService;
import com.xianyusmart.service.TokenRefreshService;
import com.xianyusmart.utils.XianyuApiCallUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderDetailFetcherTest {

    @Test
    void parsesGoodsIdentifierFromMerchantItemDetails() throws Exception {
        OrderDetailFetcher fetcher = new OrderDetailFetcher();
        OrderDetailFetcher.OrderDetailInfo detail = new OrderDetailFetcher.OrderDetailInfo();
        Map<String, Object> module = Map.of(
                "merchantItemVO", Map.of(
                        "itemId", 123456789L,
                        "title", "测试商品"
                )
        );

        Method parser = OrderDetailFetcher.class.getDeclaredMethod(
                "parseItemInfo", Map.class, OrderDetailFetcher.OrderDetailInfo.class);
        parser.setAccessible(true);
        parser.invoke(fetcher, module, detail);

        assertEquals("123456789", detail.xyGoodsId);
        assertEquals("测试商品", detail.goodsTitle);
    }

    @Test
    void refreshesH5TokenAndRetriesWhenOrderDetailTokenExpires() throws Exception {
        AccountService accountService = mock(AccountService.class);
        TokenRefreshService tokenRefreshService = mock(TokenRefreshService.class);
        XianyuApiCallUtils apiCallUtils = mock(XianyuApiCallUtils.class);
        OrderDetailFetcher fetcher = new OrderDetailFetcher();
        ReflectionTestUtils.setField(fetcher, "accountService", accountService);
        ReflectionTestUtils.setField(fetcher, "tokenRefreshService", tokenRefreshService);
        ReflectionTestUtils.setField(fetcher, "xianyuApiCallUtils", apiCallUtils);

        when(apiCallUtils.callApiWithRetry(eq(7L), eq("mtop.taobao.idle.trade.merchant.full.info"),
                anyMap(), eq("stale-cookie")))
                .thenReturn(new XianyuApiCallUtils.ApiCallResult(false, null, "令牌过期", true));
        when(tokenRefreshService.refreshMh5tkToken(7L)).thenReturn(true);
        when(accountService.getCookieByAccountId(7L)).thenReturn("fresh-cookie");
        when(apiCallUtils.callApiWithRetry(eq(7L), eq("mtop.taobao.idle.trade.merchant.full.info"),
                anyMap(), eq("fresh-cookie")))
                .thenReturn(new XianyuApiCallUtils.ApiCallResult(true, "{}", null, false));

        Method method = OrderDetailFetcher.class.getDeclaredMethod("fetchOrderDetailApi",
                Long.class, Map.class, String.class, String.class);
        method.setAccessible(true);
        XianyuApiCallUtils.ApiCallResult result = (XianyuApiCallUtils.ApiCallResult) method.invoke(
                fetcher, 7L, new HashMap<>(), "stale-cookie", "order-1");

        assertTrue(result.isSuccess());
        verify(tokenRefreshService).refreshMh5tkToken(7L);
    }
}
