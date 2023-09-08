### Example for https://github.com/awslabs/aws-advanced-jdbc-wrapper/issues/608

Without `DataSourceTransactionManagerConfig`, it is not possible to detect a
failover when the commit throws an `FailoverSQLException`. This is because the
Spring `TransactionManager` attempts to rollback in response to this exception.
The rollback fails, since it is a new connection with no active transaction, and
the rollback exception replaces the original `FailoverSQLException`.

`DataSourceTransactionManagerConfig` is workaround which overrides
`translateException` to convert a `FailoverSQLException` to a subclass of
`TransactionException`. When this exception is thrown, the Spring
`TransactionManager` does not attempt to rollback.
