FROM bitnami/wildfly:23.0.2

USER root

ADD wildfly.cli /opt/wildfly.cli
ADD mssql-jdbc-7.4.1.jre11.jar /tmp/mssql-jdbc-7.4.1.jre11.jar

RUN /opt/bitnami/wildfly/bin/add-user.sh -u 'admin' -p 'admin123#'

#RUN /opt/bitnami/wildfly/bin/jms-queue.sh add --queue-address=HL7Queue --entries=java:/jms/queue/HL7Queue

#RUN /opt/bitnami/wildfly/bin/jms-queue.sh add --queue-address=DeadLetterQueue --entries=java:/jms/queue/DeadLetterQueue

RUN /bin/sh -c '/opt/bitnami/wildfly/bin/standalone.sh -c=standalone-full.xml &' && \
sleep 10 && \
/opt/bitnami/wildfly/bin/jboss-cli.sh --connect --file=/opt/wildfly.cli

#RUN /opt/bitnami/wildfly/bin/jboss-cli.sh --file=/opt/wildfly.cli

# Expose the management port
EXPOSE 8080 9990

# Ensure the standalone.sh script is present
ENTRYPOINT ["/opt/bitnami/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
