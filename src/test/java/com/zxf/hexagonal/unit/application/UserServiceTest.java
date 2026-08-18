package com.zxf.hexagonal.unit.application;

import com.zxf.hexagonal.application.dto.CreateUserCommand;
import com.zxf.hexagonal.application.dto.UpdateUserCommand;
import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.application.port.out.EventPublisher;
import com.zxf.hexagonal.application.port.out.UserRepository;
import com.zxf.hexagonal.application.service.UserService;
import com.zxf.hexagonal.domain.event.DomainEvent;
import com.zxf.hexagonal.domain.event.UserCreatedEvent;
import com.zxf.hexagonal.domain.exception.EmailAlreadyExistsException;
import com.zxf.hexagonal.domain.exception.UserAlreadyInactiveException;
import com.zxf.hexagonal.domain.exception.UserNotFoundException;
import com.zxf.hexagonal.domain.exception.UserVersionConflictException;
import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private static User persistedUser(UserStatus status) {
        return User.restore(new UserId(1L), "Alice", new Email("alice@example.com"), status, 0L);
    }

    @Test
    void create_success_publishesUserCreatedEvent() {
        // Given
        when(userRepository.findByEmail(new Email("bob@example.com"))).thenReturn(Optional.empty());
        User saved = User.restore(new UserId(10L), "Bob", new Email("bob@example.com"),
                UserStatus.ACTIVE, 0L);
        when(userRepository.save(any())).thenReturn(saved);

        // When
        UserDto result = userService.create(new CreateUserCommand("Bob", "bob@example.com"));

        // Then
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UserCreatedEvent.class);
        assertThat(((UserCreatedEvent) captor.getValue()).userId()).isEqualTo(10L);
    }

    @Test
    void create_emailAlreadyExists_throwsAndSkipsEvent() {
        when(userRepository.findByEmail(new Email("alice@example.com")))
                .thenReturn(Optional.of(persistedUser(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> userService.create(
                new CreateUserCommand("Alice", "alice@example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(eventPublisher, never()).publish(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(new UserId(999L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void update_versionMismatch_throwsConflict() {
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(persistedUser(UserStatus.ACTIVE)));

        assertThatThrownBy(() -> userService.update(
                new UpdateUserCommand(1L, 99L, "New Name", null)))
                .isInstanceOf(UserVersionConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_emailOwnedByOther_throwsConflict() {
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(persistedUser(UserStatus.ACTIVE)));
        when(userRepository.existsByEmailAndIdNot(
                new Email("bob@example.com"), new UserId(1L))).thenReturn(true);

        assertThatThrownBy(() -> userService.update(
                new UpdateUserCommand(1L, 0L, null, "bob@example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void update_partialUpdate_ignoresNullFields() {
        User updated = User.restore(new UserId(1L), "Alice Chen",
                new Email("alice@example.com"), UserStatus.ACTIVE, 1L);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(persistedUser(UserStatus.ACTIVE)));
        when(userRepository.save(any())).thenReturn(updated);

        UserDto result = userService.update(new UpdateUserCommand(1L, 0L, "Alice Chen", null));

        // email 为 null 表示不更新：领域对象上仍为原邮箱
        assertThat(result.name()).isEqualTo("Alice Chen");
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.version()).isEqualTo(1L);
    }

    @Test
    void changeStatus_deactivateInactiveUser_throws() {
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(persistedUser(UserStatus.INACTIVE)));

        assertThatThrownBy(() -> userService.changeStatus(1L, UserStatus.INACTIVE))
                .isInstanceOf(UserAlreadyInactiveException.class);
    }

    @Test
    void changeStatus_activate_persistsNewStatus() {
        User activated = User.restore(new UserId(1L), "Alice",
                new Email("alice@example.com"), UserStatus.ACTIVE, 1L);
        when(userRepository.findById(new UserId(1L)))
                .thenReturn(Optional.of(persistedUser(UserStatus.INACTIVE)));
        when(userRepository.save(any())).thenReturn(activated);

        UserDto result = userService.changeStatus(1L, UserStatus.ACTIVE);

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void delete_delegatesToRepository() {
        userService.delete(1L);

        verify(userRepository).deleteById(new UserId(1L));
    }
}
