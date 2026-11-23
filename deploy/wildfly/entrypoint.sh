#!/bin/bash
set -e

# WARs temporär aus deployments verschieben damit WildFly ohne sie startet
mkdir -p /tmp/wars
mv /opt/jboss/wildfly/standalone/deployments/*.war /tmp/wars/ 2>/dev/null || true

/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0 -bmanagement 0.0.0.0 &
WILDFLY_PID=$!

# Warten bis WildFly Management-Interface bereit ist
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="version" > /dev/null 2>&1; do
  echo "Warte auf WildFly Management-Interface..."
  sleep 3
done

echo "WildFly bereit – konfiguriere Datasource und JMS..."
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --file=/opt/jboss/wildfly/configure.cli

echo "Konfiguration abgeschlossen – deploye WARs..."
mv /tmp/wars/*.war /opt/jboss/wildfly/standalone/deployments/

wait $WILDFLY_PID
