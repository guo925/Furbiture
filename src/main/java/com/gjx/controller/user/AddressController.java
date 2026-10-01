package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.dto.request.AddressRequest;
import com.gjx.entity.Address;
import com.gjx.service.IAddressService;
import com.gjx.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址控制器
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "地址管理", description = "地址相关接口")
@RequiredArgsConstructor
public class AddressController {

    private final AuthenticationUtil authUtil;

    private final IAddressService addressService;

    /**
     * 获取用户地址列表
     * @param request HTTP请求
     * @return 地址列表
     */
    @Operation(summary = "获取用户地址列表", description = "获取当前用户的收货地址列表")
    @GetMapping
    public R<List<Address>> list(HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        List<Address> addresses = addressService.listByUserId(userId);
        return R.ok(addresses);
    }

    /**
     * 添加地址
     * @param addressRequest 地址信息（DTO，手机号等格式由 @Valid 校验）
     * @param request HTTP请求
     * @return 添加结果
     */
    @Operation(summary = "添加地址", description = "添加新的收货地址")
    @PostMapping
    public ResponseEntity<R<?>> add(@Valid @RequestBody AddressRequest addressRequest, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        // “保存地址 + 必要时置默认”是一个事务单元，下沉到 Service，Controller 不再手工拼两步
        addressService.saveForUser(userId, addressRequest);
        return ResponseEntity.ok(R.ok("添加成功"));
    }

    /**
     * 更新地址
     * @param id 地址ID
     * @param addressRequest 地址信息（DTO）
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新地址", description = "更新收货地址信息")
    @PutMapping("/{id}")
    public ResponseEntity<R<?>> update(@PathVariable Long id,
                                       @Valid @RequestBody AddressRequest addressRequest,
                                       HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        // 归属校验（userId 进 WHERE）与“更新 + 必要时置默认”的原子性都在 Service 内完成
        addressService.updateForUser(userId, id, addressRequest);
        return ResponseEntity.ok(R.ok("更新成功"));
    }

    /**
     * 删除地址
     * @param id 地址ID
     * @param request HTTP请求
     * @return 删除结果
     */
    @Operation(summary = "删除地址", description = "删除收货地址")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        addressService.delete(id, userId);
        return R.ok("删除成功");
    }

    /**
     * 设置默认地址
     * @param id 地址ID
     * @param request HTTP请求
     * @return 设置结果
     */
    @Operation(summary = "设置默认地址", description = "设置默认收货地址")
    @PutMapping("/{id}/default")
    public R<?> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authUtil.getUserIdFromRequest(request);
        addressService.setDefault(id, userId);
        return R.ok("设置成功");
    }
}
