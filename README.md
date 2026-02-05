# WiFi Tracker

一个Android应用，用于定时扫描指定WiFi并记录打卡信息。

## 功能特性

- **WiFi扫描**: 定时扫描周围WiFi，支持部分匹配（忽略大小写）
- **时间范围设置**: 可设置扫描的开始和结束时间（整点）
- **日志记录**: 记录所有扫描到匹配WiFi的时间和WiFi名称
- **打卡统计**: 按月显示日历，统计每天匹配次数，超过3次显示绿色标记

## 技术栈

- Kotlin
- Jetpack Compose
- MVVM架构
- Room数据库
- WorkManager（定时任务）
- Material3

## 权限要求

- `ACCESS_WIFI_STATE`: 访问WiFi状态
- `CHANGE_WIFI_STATE`: 更改WiFi状态
- `ACCESS_FINE_LOCATION`: 精确位置（Android 6.0+需要）
- `ACCESS_COARSE_LOCATION`: 粗略位置（Android 6.0+需要）

## 构建说明

1. 使用Android Studio打开项目
2. 同步Gradle依赖
3. 运行应用

## 使用说明

1. 首次启动应用会请求位置权限（WiFi扫描需要）
2. 在设置页面配置：
   - WiFi名称（例如：1865）
   - 扫描开始时间（整点，例如：8点）
   - 扫描结束时间（整点，例如：20点）
3. 应用会在设置的时间范围内每小时自动扫描一次
4. 在日志页面查看所有扫描记录
5. 在打卡页面查看每月的统计信息

## 注意事项

- Android 10+对WiFi扫描有更严格的限制
- 后台任务可能被系统限制，建议关闭电池优化
- 位置权限是WiFi扫描的必要条件
