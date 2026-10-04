package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

/** Một pool/Jdbi cho ứng dụng; không chia sẻ Handle giữa request. */
public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final Jdbi jdbi;
    public Database(DatabaseConfig config) {
        HikariConfig pool = new HikariConfig();
        // Lifecycle có thể deregister driver; pool mới cần tự khởi tạo driver trong cùng JVM test.
        pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
        pool.setJdbcUrl(config.url());
        pool.setUsername(config.username());
        pool.setPassword(config.password());
        pool.setMaximumPoolSize(config.poolSize());
        pool.setMinimumIdle(0);
        pool.setConnectionTimeout(config.timeoutMs());
        pool.setInitializationFailTimeout(-1);
        pool.setPoolName("c2c-database");
        pool.setConnectionInitSql("SET time_zone = '+00:00'");
        dataSource = new HikariDataSource(pool);
        jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
    }
    public Jdbi jdbi() { return jdbi; }
    public HikariDataSource dataSource() { return dataSource; }
    public <T, X extends Exception> T read(HandleCallback<T, X> callback) throws X { return jdbi.withHandle(callback); }
    /** Service attach tất cả DAO vào Handle này; exception rollback toàn transaction. */
    public <T, X extends Exception> T transaction(HandleCallback<T, X> callback) throws X {
        return jdbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, callback);
    }
    @Override public void close() { dataSource.close(); }
}
