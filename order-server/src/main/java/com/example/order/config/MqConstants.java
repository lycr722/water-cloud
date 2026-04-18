package com.example.order.config;

public final class MqConstants {
    private MqConstants() {
    }

    public static final String COMMAND_EXCHANGE = "order.command.exchange";
    public static final String EVENT_EXCHANGE = "order.event.exchange";

    public static final String COMMAND_QUEUE = "order.command.queue";
    public static final String COMMAND_DLQ = "order.command.dlq";
    public static final String USER_NOTIFY_QUEUE = "order.notify.user.queue";
    public static final String WORKER_NOTIFY_QUEUE = "order.notify.worker.queue";

    public static final String COMMAND_KEY_CREATE = "order.command.create";
    public static final String COMMAND_KEY_ACCEPT = "order.command.accept";
    public static final String COMMAND_KEY_FINISH = "order.command.finish";
    public static final String EVENT_KEY_USER = "order.event.user";
    public static final String EVENT_KEY_WORKER = "order.event.worker";

    /** 异步削峰落库 */
    public static final String PERSIST_EXCHANGE = "order.persist.exchange";
    public static final String PERSIST_QUEUE = "order.persist.queue";
    public static final String PERSIST_KEY = "order.persist";

    /** 缓存失效广播（Binlog / 管理端） */
    public static final String CACHE_INVALIDATE_EXCHANGE = "cache.invalidate.exchange";
    public static final String CACHE_INVALIDATE_QUEUE_ORDER = "cache.invalidate.order.queue";
    public static final String CACHE_INVALIDATE_KEY = "cache.invalidate";
}
