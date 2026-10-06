package com.example.demo.listener;

import com.example.demo.config.*;
import com.example.demo.service.HealthService;
import com.example.demo.service.UserService;
import com.example.demo.controller.AccountSupport;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.Optional;

@WebListener
public final class ApplicationListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        Optional<Database> database = Optional.empty();
        boolean valid = true;
        boolean diagnostics = false;
        try {
            AppConfig config = AppConfig.load();
            diagnostics = config.diagnosticsEnabled();
            database = config.database().map(Database::new);
            database.ifPresent(db -> event.getServletContext().setAttribute(ShopServices.KEY,new ShopServices(db,config.uploadRoot(),config.vnpay())));
            if (database.isEmpty()) event.getServletContext().log(
                    "[DATABASE_NOT_CONFIGURED] Tomcat chưa có cấu hình DB. Đặt APP_CONFIG_FILE hoặc -Dc2c.config "
                    + "trong Run Configuration; biến của terminal Maven không tự truyền sang IntelliJ/Tomcat.");
        } catch (ConfigurationException exception) {
            valid = false;
            event.getServletContext().log("[DATABASE_CONFIGURATION_INVALID] " + exception.getMessage());
        } catch (IOException | RuntimeException exception) {
            valid = false;
            event.getServletContext().log("Cấu hình C2C không hợp lệ. Loại lỗi: " + exception.getClass().getSimpleName());
        }
        // Không chạy migration/seed ở lifecycle Tomcat.
        event.getServletContext().setAttribute(ApplicationRuntime.CONTEXT_KEY,
                new ApplicationRuntime(valid, diagnostics, database, new HealthService(database)));
        database.ifPresent(db -> event.getServletContext().setAttribute(AccountSupport.SERVICE_KEY, new UserService(db)));
    }
    @Override public void contextDestroyed(ServletContextEvent event) {
        var runtime = (ApplicationRuntime) event.getServletContext().getAttribute(ApplicationRuntime.CONTEXT_KEY);
        if (runtime != null) runtime.database().ifPresent(Database::close);
        JdbcLifecycle.shutdown();
    }
}
