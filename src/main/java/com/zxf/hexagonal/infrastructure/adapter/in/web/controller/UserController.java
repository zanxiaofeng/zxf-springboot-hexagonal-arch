package com.zxf.hexagonal.infrastructure.adapter.in.web.controller;

import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.application.port.in.ChangeUserStatusUseCase;
import com.zxf.hexagonal.application.port.in.CreateUserUseCase;
import com.zxf.hexagonal.application.port.in.DeleteUserUseCase;
import com.zxf.hexagonal.application.port.in.GetUserUseCase;
import com.zxf.hexagonal.application.port.in.ListUsersUseCase;
import com.zxf.hexagonal.application.port.in.UpdateUserUseCase;
import com.zxf.hexagonal.domain.model.UserStatus;
import com.zxf.hexagonal.infrastructure.adapter.in.web.common.ApiResponse;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.ChangeUserStatusRequest;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.CreateUserRequest;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.UserResponse;
import com.zxf.hexagonal.infrastructure.adapter.in.web.mapper.UserWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 用户入站适配器：仅做 HTTP 协议转换，零业务逻辑；依赖入端口接口而非实现。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserDto dto = createUserUseCase.create(UserWebMapper.toCommand(request));
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + dto.id()))
                .body(ApiResponse.success(UserWebMapper.toResponse(dto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                UserWebMapper.toResponse(getUserUseCase.findById(id))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                UserWebMapper.toResponsePage(listUsersUseCase.list(name, status, pageable))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDto dto = updateUserUseCase.update(UserWebMapper.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success(UserWebMapper.toResponse(dto)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ChangeUserStatusRequest request) {
        UserDto dto = changeUserStatusUseCase.changeStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success(UserWebMapper.toResponse(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        deleteUserUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
