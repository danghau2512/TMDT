package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.HealthDao;
import com.example.demo.model.DatabaseHealth;
import java.util.Optional;

public final class HealthService {
    private final Optional<Database> database;
    public HealthService(Optional<Database> database) { this.database = database; }
    public DatabaseHealth checkDatabase() {
        if (database.isEmpty()) return new DatabaseHealth(DatabaseHealth.State.NOT_CONFIGURED, "Chưa cấu hình MySQL. Xem README để thiết lập kết nối.");
        try {
            if (database.get().read(handle -> handle.attach(HealthDao.class).ping()) == 1) {
                return new DatabaseHealth(DatabaseHealth.State.CONNECTED, "Kết nối MySQL qua JDBI thành công (SELECT 1 = 1).");
            }
        } catch (RuntimeException ignored) { /* Không tiết lộ URL/SQL/credential trong exception. */ }
        return new DatabaseHealth(DatabaseHealth.State.UNAVAILABLE, "Không kết nối được MySQL. Kiểm tra dịch vụ và cấu hình cục bộ.");
    }
}
