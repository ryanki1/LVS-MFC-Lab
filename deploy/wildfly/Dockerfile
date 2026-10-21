FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk21

USER root

ARG POSTGRES_JDBC_VERSION=42.7.5

RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main
ADD https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_JDBC_VERSION}/postgresql-${POSTGRES_JDBC_VERSION}.jar /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar

COPY deploy/wildfly/postgresql-module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml
COPY deploy/wildfly/configure.cli /opt/jboss/wildfly/configure.cli
COPY services/lvs-service/target/lvs-service-1.0.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/lvs-service.war
COPY services/mfr-service/target/mfr-service-1.0.0-SNAPSHOT.war /opt/jboss/wildfly/standalone/deployments/mfr-service.war

RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/configure.cli

RUN chown -R jboss:root /opt/jboss/wildfly/standalone /opt/jboss/wildfly/modules/org/postgresql

USER jboss

EXPOSE 8080 9990
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
