#!/bin/bash
# Wait for the SQL Server to come up
sleep 30s

# Run the SQL script to create a new database
/opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P 'Sql@2024Pass' -Q 'CREATE DATABASE gravitee'