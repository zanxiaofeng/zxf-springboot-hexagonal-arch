package com.zxf.hexagonal.application.service;

import com.zxf.hexagonal.application.dto.CreateUserCommand;
import com.zxf.hexagonal.application.dto.UpdateUserCommand;
import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.application.port.in.ChangeUserStatusUseCase;
import com.zxf.hexagonal.application.port.in.CreateUserUseCase;
import com.zxf.hexagonal.application.port.in.DeleteUserUseCase;
import com.zxf.hexagonal.application.port.in.GetUserUseCase;
import com.zxf.hexagonal.application.port.in.ListUsersUseCase;
import com.zxf.hexagonal.application.port.in.UpdateUserUseCase;
import com.zxf.hexagonal.application.port.out.EventPublisher;
import com.zxf.hexagonal.application.port.out.UserRepository;
import com.zxf.hexagonal.domain.event.UserCreatedEvent;
import com.zxf.hexagonal.domain.exception.EmailAlreadyExistsException;
import com.zxf.hexagonal.domain.exception.UserNotFoundException;
import com.zxf.hexagonal.domain.exception.UserVersionConflictException;
import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 用户应用服务：用例编排。业务规则在领域层（User/Email），本层只做
 * Command → 领域对象 → 出端口 → DTO 的编排与事务边界。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements CreateUserUseCase, GetUserUseCase, ListUsersUseCase,
        UpdateUserUseCase, ChangeUserStatusUseCase, DeleteUserUseCase {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto create(CreateUserCommand command) {
        Email email = new Email(command.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(command.email());
        }
        User saved = userRepository.save(User.create(command.name(), email));
        // 事务内仅注册意图，适配器保证 afterCommit 后才真正外发（见 LocalEventPublisher）
        eventPublisher.publish(new UserCreatedEvent(
                saved.getId().value(), saved.getEmail().value(), OffsetDateTime.now()));
        return UserDto.from(saved);
    }

    @Override
    public UserDto findById(Long id) {
        return UserDto.from(requireUser(id));
    }

    @Override
    public Page<UserDto> list(String name, UserStatus status, Pageable pageable) {
        return userRepository.findAll(name, status, pageable).map(UserDto::from);
    }

    @Override
    @Transactional
    public UserDto update(UpdateUserCommand command) {
        User user = requireUser(command.id());
        if (!Objects.equals(user.getVersion(), command.version())) {
            throw new UserVersionConflictException(command.id());
        }
        if (command.email() != null) {
            Email newEmail = new Email(command.email());
            if (userRepository.existsByEmailAndIdNot(newEmail, user.getId())) {
                throw new EmailAlreadyExistsException(command.email());
            }
            user.changeEmail(newEmail);
        }
        if (command.name() != null) {
            user.changeName(command.name());
        }
        return UserDto.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto changeStatus(Long id, UserStatus target) {
        User user = requireUser(id);
        // 非法转换（如已停用再停用）由领域方法抛 UserAlreadyInactiveException
        if (target == UserStatus.ACTIVE) {
            user.activate();
        } else {
            user.deactivate();
        }
        return UserDto.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 不存在（或已删除）时由适配器抛 UserNotFoundException
        userRepository.deleteById(new UserId(id));
    }

    private User requireUser(Long id) {
        return userRepository.findById(new UserId(id))
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
