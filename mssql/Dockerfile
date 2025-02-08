FROM mcr.microsoft.com/mssql/server:2019-latest

USER root

RUN /bin/sh -c '/opt/mssql/bin/mssql-conf set sqlagent.enabled true'

EXPOSE 1433

ENTRYPOINT ["/opt/mssql/bin/sqlservr"]
