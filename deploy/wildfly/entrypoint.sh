#!/bin/bash
set -e

# Datasource und JMS zur Laufzeit konfigurieren damit DB-Umgebungsvariablen verfügbar sind
/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0 -bmanagement 0.0.0.0 &
WILDFLY_PID=$!

# Warten bis WildFly Management-Interface bereit ist
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="version" > /dev/null 2>&1; do
  echo "Warte auf WildFly Management-Interface..."
  sleep 3
done

echo "WildFly bereit – konfiguriere Datasource..."
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --file=/opt/jboss/wildfly/configure.cli

echo "Konfiguration abgeschlossen."
wait $WILDFLY_PID
