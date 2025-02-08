# demo-cdc

to demonstrate change data capture (CDC) in SQL Server.

* a java application acts as a scheduled task to periodically insert rows into a table in SQL Server
* a java application acts as a scheduled task to periodically read CDC events for that table  in SQL Server and send a JMS message for each with the CDC event details (acts as a JMS sender)
* a java application acts as a JMS message reciver and logs the event details from each message

![CDC Diagram](docs/cdc-diagram.png)

