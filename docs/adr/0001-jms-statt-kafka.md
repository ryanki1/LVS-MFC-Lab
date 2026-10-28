Kontext

Punkt-zu-Punkt Messaging zwischen LVS und MFR nötig
Aufträge müssen zuverlässig genau einmal fachlich wirksam werden

Entscheidung

JMS-Queues im eingebetteten ActiveMQ Artemis von WildFly

Vergleich

JMS unterstützt WildFlys XA-Transaktionen – DB und JMS atomar in einer JTA-Transaktion
Kafka unterstützt kein XA – separate Transaktion, kein atomares Rollback über DB + Queue
Kafka als Ereignis-Log sinnvoll bei vielen Konsumenten; hier nur ein Konsument pro Nachricht

Konsequenzen

Durable Messaging: Nachrichten auf Disk persistiert, überleben Stromausfall
Redelivery bei Fehlern → idempotente Verarbeitung nötig
DLQ für dauerhaft fehlerhafte Nachrichten