package com.example.awsadvancedtransactionexample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestService {
  private final JdbcTemplate jdbcTemplate;

  public TestService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public int selectOne() {
    return jdbcTemplate.queryForObject("select 1", Integer.class);
  }
}
