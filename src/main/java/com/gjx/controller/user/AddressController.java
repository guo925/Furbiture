package com.gjx.controller.user;

import com.gjx.common.R;
import com.gjx.common.ResultCode;
import com.gjx.entity.Address;
import com.gjx.service.IAddressService;
import com.gjx.util.AuthenticationUtil;
import com.gjx.util.ValidationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址控制器
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "地址管理", description = "地址相关接口")
public class AddressController {

    @Autowired
    private IAddressService addressService;

    /**
     * 获取用户地址列表
     * @param request HTTP请求
     * @return 地址列表
     */
    @Operation(summary = "获取用户地址列表", description = "获取当前用户的收货地址列表")
    @GetMapping
    public R<List<Address>> list(HttpServletRequest request) {
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        List<Address> addresses = addressService.listByUserId(userId);
        return R.ok(addresses);
    }

    /**
     * 添加地址
     * @param address 地址信息
     * @param request HTTP请求
     * @return 添加结果
     */
    @Operation(summary = "添加地址", description = "添加新的收货地址")
    @PostMapping
    public ResponseEntity<R<?>> add(@RequestBody Address address, HttpServletRequest request) {
        // 验证手机号码格式
        if (!ValidationUtil.isValidPhone(address.getPhone())) {
            R<?> errorResponse = R.error(ResultCode.PARAM_ERROR, "手机号码格式不正确，请输入11位中国大陆手机号码");
            return ResponseEntity.status(ResultCode.PARAM_ERROR.getCode()).body(errorResponse);
        }
        
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        address.setUserId(userId);
        addressService.save(address);
        return ResponseEntity.ok(R.ok("添加成功"));
    }

    /**
     * 更新地址
     * @param id 地址ID
     * @param address 地址信息
     * @param request HTTP请求
     * @return 更新结果
     */
    @Operation(summary = "更新地址", description = "更新收货地址信息")
    @PutMapping("/{id}")
    public ResponseEntity<R<?>> update(@PathVariable Long id, @RequestBody Address address, HttpServletRequest request) {
        // 验证手机号码格式
        if (!ValidationUtil.isValidPhone(address.getPhone())) {
            R<?> errorResponse = R.error(ResultCode.PARAM_ERROR, "手机号码格式不正确，请输入11位中国大陆手机号码");
            return ResponseEntity.status(ResultCode.PARAM_ERROR.getCode()).body(errorResponse);
        }
        
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        address.setId(id);
        address.setUserId(userId);
        addressService.updateById(address);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
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
        Long userId = AuthenticationUtil.getUserIdFromRequest(request);
        addressService.setDefault(id, userId);
        return R.ok("设置成功");
    }
}