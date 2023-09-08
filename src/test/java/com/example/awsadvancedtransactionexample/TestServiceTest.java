package com.example.awsadvancedtransactionexample;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.jdbc.plugin.failover.FailoverSuccessSQLException;

@SpringBootTest
@Testcontainers
class TestServiceTest {
  @Autowired
  TestService testService;
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15");
  @SpyBean
  DataSource dataSource;

  @DynamicPropertySource
  static void dataSourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url",
        () -> postgreSQLContainer.getJdbcUrl().replace("jdbc:postgresql", "jdbc:aws-wrapper:postgresql"));
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
  }

  @Test
  void testSelectOne() {
    assertEquals(1, testService.selectOne());
  }

  @Test
  void testSelectOneWithFailover() throws Exception {
    var connections = failoverOnCommit();
    // the following line fails when DataSourceTransactionManagerConfig isn't present, in a way that
    // suppresses the original FailoverSQLException (since it attempts to call rollback() after the failed commit())
    assertThrows(DataSourceTransactionManagerConfig.FailoverDuringTransactionException.class, testService::selectOne);
    assertEquals(1, connections.size());
    verify(connections.get(0), times(1)).commit();
    verify(connections.get(0), never()).rollback();
  }

  private List<Connection> failoverOnCommit() throws Exception {
    var connections = new ArrayList<Connection>();
    doAnswer(inv -> {
      var proxy = spy((Connection) inv.callRealMethod());
      doThrow(new FailoverSuccessSQLException()).when(proxy).commit();
      // https://github.com/pgjdbc/pgjdbc/blob/7725a8e7894f17d0fece7db53c87d3b080b72c26/pgjdbc/src/main/java/org/postgresql/jdbc/PgConnection.java#L1003
      doThrow(new PSQLException(GT.tr("Cannot rollback when autoCommit is enabled."), PSQLState.NO_ACTIVE_SQL_TRANSACTION))
          .when(proxy).rollback();
      connections.add(proxy);
      return proxy;
    }).when(dataSource).getConnection();
    return connections;
  }
}