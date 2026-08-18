package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.exception.UserAlreadyInactiveException;
import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User persisted(UserStatus status) {
        return User.restore(new UserId(1L), "Alice", new Email("alice@example.com"), status, 0L);
    }

    @Test
    void create_newUserIsActiveAndNotPersisted() {
        User user = User.create("Alice", new Email("alice@example.com"));

        assertThat(user.getId()).isNull();
        assertThat(user.getVersion()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void restore_requiresIdAndVersion() {
        assertThatThrownBy(() -> User.restore(null, "Alice",
                new Email("alice@example.com"), UserStatus.ACTIVE, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.restore(new UserId(1L), "Alice",
                new Email("alice@example.com"), UserStatus.ACTIVE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsInvalidName() {
        assertThatThrownBy(() -> User.create("A", new Email("a@example.com")))
                .isInstanceOf(IllegalArgumentException.class);   // 太短
        assertThatThrownBy(() -> User.create(" ", new Email("a@example.com")))
                .isInstanceOf(IllegalArgumentException.class);   // 空白
        assertThatThrownBy(() -> User.create(null, new Email("a@example.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeName_updatesWhenValid() {
        User user = persisted(UserStatus.ACTIVE);

        user.changeName("Alice Chen");

        assertThat(user.getName()).isEqualTo("Alice Chen");
    }

    @Test
    void deactivate_changesStatusToInactive() {
        User user = persisted(UserStatus.ACTIVE);

        user.deactivate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void deactivate_rejectsWhenAlreadyInactive() {
        User user = persisted(UserStatus.INACTIVE);

        assertThatThrownBy(user::deactivate)
                .isInstanceOf(UserAlreadyInactiveException.class);
    }

    @Test
    void activate_isIdempotent() {
        User user = persisted(UserStatus.ACTIVE);

        user.activate();

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
