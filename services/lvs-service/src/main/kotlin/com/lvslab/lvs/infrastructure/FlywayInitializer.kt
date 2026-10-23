package com.lvslab.lvs.infrastructure

import jakarta.annotation.PostConstruct
import jakarta.ejb.Singleton
import jakarta.ejb.Startup
import org.flywaydb.core.Flyway

@Singleton
@Startup
class FlywayInitializer {

    // Flyway bekommt eine eigene JDBC-URL, weil WildFlys JTA-Datasource kein Auto-Commit erlaubt.
    // DB-Koordinaten kommen aus denselben Umgebungsvariablen wie die WildFly-Datasource.
    @PostConstruct
    fun init() {
        val host = System.getenv("DB_HOST") ?: "postgres"
        val db   = System.getenv("DB_NAME") ?: "lvslab"
        val user = System.getenv("DB_USER") ?: error("DB_USER not set")
        val pass = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD not set")

        Flyway.configure()
            .dataSource("jdbc:postgresql://$host:5432/$db", user, pass)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
