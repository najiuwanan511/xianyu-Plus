package com.xianyusmart.service;

import com.xianyusmart.entity.XianyuGoodsAutoDeliveryConfig;
import com.xianyusmart.entity.XianyuGoodsOrder;
import com.xianyusmart.event.chatMessageEvent.ChatMessageData;

public interface ZeroBridgeService {
    boolean isEnabled();
    boolean hasActiveSession(Long accountId, String sid, String buyerId);
    void initializeCollection(XianyuGoodsOrder order, XianyuGoodsAutoDeliveryConfig config, int buyNum);
    boolean collectBuyerMessage(ChatMessageData message);
    boolean testConnection();
    CallbackResult acceptCallback(String rawBody, String eventId, String timestamp, String signature);

    record CallbackResult(int httpStatus, String message) {}
}
