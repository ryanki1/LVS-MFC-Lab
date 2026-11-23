#!/bin/bash

# WARs temporär aus deployments verschieben damit WildFly ohne sie startet
mkdir -p /tmp/wars
mv /opt/jboss/wildfly/standalone/deployments/*.war /tmp/wars/ 2>/dev/null || true

/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0 -bmanagement 0.0.0.0 &
WILDFLY_PID=$!

# Warten bis WildFly server-state=running meldet
echo "Warte bis WildFly bereit ist..."
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --controller=127.0.0.1:9990 \
  --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q '"result" => "running"'; do
  echo "Noch nicht bereit, warte..."
  sleep 3
done

echo "WildFly bereit – konfiguriere Datasource und JMS..."
while true; do
  CLI_OUTPUT=$(/opt/jboss/wildfly/bin/jboss-cli.sh --connect --controller=127.0.0.1:9990 \
    --file=/opt/jboss/wildfly/configure-runtime.cli 2>&1)
  if echo "$CLI_OUTPUT" | grep -q "The command is not available"; then
    echo "CLI noch nicht verfügbar, warte..."
    sleep 3
  else
    echo "$CLI_OUTPUT"
    break
  fi
done

echo "Konfiguration abgeschlossen – deploye WARs..."
mv /tmp/wars/*.war /opt/jboss/wildfly/standalone/deployments/

wait $WILDFLY_PID
