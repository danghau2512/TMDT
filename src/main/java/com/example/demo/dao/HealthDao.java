package com.example.demo.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface HealthDao {
    @SqlQuery("SELECT 1") int ping();
    @SqlQuery("SELECT :value") String echo(@Bind("value") String value);
}
