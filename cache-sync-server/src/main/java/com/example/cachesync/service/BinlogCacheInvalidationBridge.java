package com.example.cachesync.service;

import com.example.cachesync.config.BinlogProperties;
import com.example.cachesync.config.RabbitOutProperties;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.rows.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.rows.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.rows.WriteRowsEventData;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 伪装从库订阅 Binlog，将数据变更转为缓存失效 MQ（需 MySQL 开启 binlog=ROW，并授予 REPLICATION SLAVE/CLIENT）。
 */
@Component
public class BinlogCacheInvalidationBridge {
    private final BinlogProperties binlogProperties;
    private final RabbitOutProperties rabbitOutProperties;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    private final Map<Long, String> tableIdToName = new ConcurrentHashMap<>();
    private BinaryLogClient client;
    private ExecutorService executor;

    public BinlogCacheInvalidationBridge(
            BinlogProperties binlogProperties,
            RabbitOutProperties rabbitOutProperties,
            RabbitTemplate rabbitTemplate,
            RabbitAdmin rabbitAdmin) {
        this.binlogProperties = binlogProperties;
        this.rabbitOutProperties = rabbitOutProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!binlogProperties.isEnabled()) {
            return;
        }
        rabbitAdmin.declareExchange(new DirectExchange(rabbitOutProperties.getInvalidateExchange(), true, false));
        client = new BinaryLogClient(
                binlogProperties.getHost(),
                binlogProperties.getPort(),
                binlogProperties.getUsername(),
                binlogProperties.getPassword());
        client.setServerId(binlogProperties.getServerId());
        client.registerEventListener(this::onEvent);
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mysql-binlog-listener");
            t.setDaemon(true);
            return t;
        });
        executor.execute(() -> {
            try {
                client.connect();
            } catch (IOException e) {
                System.err.println("Binlog 连接失败: " + e.getMessage());
            }
        });
    }

    private void onEvent(Event event) {
        EventData data = event.getData();
        if (data instanceof TableMapEventData) {
            TableMapEventData t = (TableMapEventData) data;
            tableIdToName.put(t.getTableId(), t.getTable());
            return;
        }
        if (data instanceof WriteRowsEventData || data instanceof UpdateRowsEventData || data instanceof DeleteRowsEventData) {
            long tableId = extractTableId(data);
            String table = tableIdToName.get(tableId);
            if (table != null && (table.contains("water_product") || table.contains("t_building"))) {
                String payload = "{\"table\":\"" + table + "\",\"op\":\"" + data.getClass().getSimpleName() + "\"}";
                rabbitTemplate.convertAndSend(
                        rabbitOutProperties.getInvalidateExchange(),
                        rabbitOutProperties.getInvalidateRoutingKey(),
                        payload);
            }
        }
    }

    private long extractTableId(EventData data) {
        if (data instanceof WriteRowsEventData) {
            return ((WriteRowsEventData) data).getTableId();
        }
        if (data instanceof UpdateRowsEventData) {
            return ((UpdateRowsEventData) data).getTableId();
        }
        if (data instanceof DeleteRowsEventData) {
            return ((DeleteRowsEventData) data).getTableId();
        }
        return -1L;
    }

    @PreDestroy
    public void stop() throws IOException {
        if (client != null) {
            client.disconnect();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
