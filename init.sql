USE master;
GO

-- Enable SQL Server Agent
EXEC sp_configure 'Agent XPs', 1;
GO
RECONFIGURE
GO

CREATE DATABASE testdb;
GO

USE testdb;
GO

-- Create table to store XML documents
CREATE TABLE HL7Events (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    EventType NVARCHAR(50) NOT NULL,
    EventData XML NOT NULL,
    CreatedAt DATETIME DEFAULT GETDATE()
);
GO

-- Enable CDC for the database
EXEC sys.sp_cdc_enable_db;
GO

-- Enable CDC for the HL7Events table
EXEC sys.sp_cdc_enable_table
    @source_schema = 'dbo',
    @source_name = 'HL7Events',
    @role_name = NULL,
    @supports_net_changes = 1;
GO

-- Create table to store the last processed LSN
CREATE TABLE ProcessedLSN (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    LastLSN BINARY(10) NOT NULL,
    ProcessedAt DATETIME DEFAULT GETDATE()
);
GO

-- Insert the result of sys.fn_cdc_get_min_lsn into ProcessedLSN
DECLARE @min_lsn BINARY(10);
SET @min_lsn = sys.fn_cdc_get_min_lsn('dbo_HL7Events');

INSERT INTO ProcessedLSN (LastLSN) VALUES (@min_lsn);
GO