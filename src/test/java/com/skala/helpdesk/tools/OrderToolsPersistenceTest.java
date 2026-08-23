package com.skala.helpdesk.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.skala.helpdesk.service.OrderNotFoundException;

@SpringBootTest
@Transactional
class OrderToolsPersistenceTest {
  @Autowired
  OrderTools tools;

  @Test
  void 실제_DB에서_본인_주문만_조회한다() {
    assertThat(tools.getOrder("99999", context("user2")).orderId()).isEqualTo("99999");

    assertThatThrownBy(() -> tools.getOrder("99999", context("user1")))
        .isInstanceOf(OrderNotFoundException.class);
  }

  private ToolContext context(String userId) {
    return new ToolContext(Map.of("userId", userId));
  }
}
