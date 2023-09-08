package com.example.awsadvancedtransactionexample;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionException;
import software.amazon.jdbc.plugin.failover.FailoverSQLException;

@Configuration
public class DataSourceTransactionManagerConfig {
  @Bean
  DataSourceTransactionManager transactionManager(DataSource dataSource,
                                                  ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
    var transactionManager = new JdbcTransactionManager(dataSource) {
      @Override
      protected RuntimeException translateException(String task, SQLException ex) {
        if (ex instanceof FailoverSQLException) {
          // We need to translate this to a subclass of `TransactionException` so that the TransactionManager doesn't try to roll back
          throw new FailoverDuringTransactionException(task + " failed", ex);
        }
        return super.translateException(task, ex);
      }
    };
    transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
    return transactionManager;
  }

  static class FailoverDuringTransactionException extends TransactionException {
    public FailoverDuringTransactionException(String s, SQLException ex) {
      super(s, ex);
    }
  }
}
