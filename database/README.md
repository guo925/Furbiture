# 家具商城数据库初始化指南

## 数据库配置

项目使用MySQL数据库，默认配置信息如下：

- 数据库名称：`furniture_db`
- 端口：`3306`
- 用户名：`root`
- 密码：`123456`

## 初始化步骤

### 1. 创建数据库

```sql
CREATE DATABASE furniture_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 执行表结构脚本

执行 `database/schema.sql` 文件中的SQL语句，创建所有表并插入测试数据。

### 3. 验证数据库

检查以下表是否创建成功：
- `user` - 用户表
- `category` - 分类表
- `product` - 商品表
- `product_image` - 商品图片表
- `product_spec` - 商品规格表
- `address` - 地址表
- `cart` - 购物车表
- `order` - 订单表
- `order_item` - 订单项表

## 测试账号

系统预置了以下测试账号：

### 管理员账号
- 用户名：`admin`
- 密码：`123456`
- 角色：`ADMIN`

### 普通用户账号
- 用户名：`user1`
- 密码：`123456`
- 角色：`USER`

## 表结构说明

### 用户表 (user)
存储用户基本信息，包括用户名、密码、联系方式等。

### 分类表 (category)
存储商品分类信息，支持多级分类结构。

### 商品表 (product)
存储商品基本信息，包括名称、价格、库存、状态等。

### 商品图片表 (product_image)
存储商品的多张图片信息。

### 商品规格表 (product_spec)
存储商品的规格信息，如颜色、尺寸等。

### 地址表 (address)
存储用户的收货地址信息。

### 购物车表 (cart)
存储用户的购物车商品信息。

### 订单表 (order)
存储订单基本信息，包括订单号、状态、金额等。

### 订单项表 (order_item)
存储订单中的商品明细信息。

## 注意事项

1. 确保MySQL服务已启动
2. 确保数据库连接配置正确（查看 `application.yaml` 文件）
3. 如果修改了数据库密码，请同步更新 `application.yaml` 中的配置
4. 测试账号的密码已使用BCrypt加密，实际使用时需要相应的加密处理
