package com.example.demo.config;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Dọn thread/driver do classloader ứng dụng sở hữu khi undeploy hoặc CLI kết thúc. */
public final class JdbcLifecycle {
    private JdbcLifecycle() { }
    public static void shutdown() {
        AbandonedConnectionCleanupThread.checkedShutdown();
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == JdbcLifecycle.class.getClassLoader()) {
                try { DriverManager.deregisterDriver(driver); }
                catch (SQLException ignored) { /* Không log credential qua cause. */ }
            }
        }
    }
}
