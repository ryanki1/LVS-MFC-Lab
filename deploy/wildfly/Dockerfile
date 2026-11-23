# Stage 1 – Build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY services/lvs-service/pom.xml services/lvs-service/
COPY services/mfr-service/pom.xml services/mfr-service/
RUN mvn dependency:go-offline -pl services/lvs-service,services/mfr-service
COPY services/lvs-service/src services/lvs-service/src
COPY services/mfr-service/src services/mfr-service/src
RUN mvn package -DskipTests -pl services/lvs-service,services/mfr-service

# Stage 2 – Runtime
FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk21

USER root

ARG POSTGRES_JDBC_VERSION=42.7.5

RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main
ADD https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_JDBC_VERSION}/postgresql-${POSTGRES_JDBC_VERSION}.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar

COPY deploy/wildfly/postgresql-module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY deploy/wildfly/configure.cli /opt/jboss/wildfly/configure.cli
COPY deploy/wildfly/configure-runtime.cli /opt/jboss/wildfly/configure-runtime.cli
COPY deploy/wildfly/entrypoint.sh /opt/jboss/wildfly/entrypoint.sh
COPY --from=builder /build/services/lvs-service/target/lvs-service-1.0.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/lvs-service.war
COPY --from=builder /build/services/mfr-service/target/mfr-service-1.0.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/mfr-service.war

RUN chown -R jboss:root /opt/jboss/wildfly/standalone /opt/jboss/wildfly/modules/org/postgresql \
    && chmod +x /opt/jboss/wildfly/entrypoint.sh

USER jboss

ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx384m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/jboss/wildfly/standalone/log/heap.hprof"

EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/entrypoint.sh"]
