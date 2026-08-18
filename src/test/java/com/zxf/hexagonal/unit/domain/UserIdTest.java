package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void constructor_acceptsPositiveNumber() {
        assertThat(new UserId(1L).value()).isEqualTo(1L);
    }

    @Test
    void constructor_rejectsNullOrNonPositive() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserId(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UserId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
