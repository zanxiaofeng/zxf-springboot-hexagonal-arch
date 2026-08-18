package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.model.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void constructor_acceptsValidEmail() {
        Email email = new Email("alice@example.com");

        assertThat(email.value()).isEqualTo("alice@example.com");
    }

    @Test
    void constructor_rejectsInvalidFormats() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("plain-address"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("a@b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("user@domain.123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
