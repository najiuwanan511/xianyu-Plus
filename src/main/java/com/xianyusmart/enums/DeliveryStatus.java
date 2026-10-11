package com.xianyusmart.enums;

public enum DeliveryStatus {
    PENDING,
    PROCESSING,
    RETRY_WAIT,
    DELIVERED,
    CONFIRMING,
    COMPLETED,
    FAILED,
    REVIEW_REQUIRED,
    SKIPPED,
    /**
     * 凭证无法自动恢复时的挂起状态：调度器不再领取该任务，
     * 等待人工重新登录/手动重试。由 DeliveryTaskServiceImpl 在买家身份核验
     * 连续失败且 Token 刷新未生效时设置。
     */
    SUSPENDED
}
