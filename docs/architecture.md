# WiFi Tracker 架构设计文档

## 项目概述

WiFi Tracker 是一个Android应用，用于定时扫描指定WiFi并记录打卡信息。应用采用MVVM架构，使用Jetpack Compose构建UI，Room数据库存储数据。

## 技术栈

- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **数据库**: Room
- **后台任务**: WorkManager
- **最低SDK**: API 30 (Android 11)
- **目标SDK**: API 34 (Android 14)

## 架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│           UI Layer                   │
│  (Compose Screens + ViewModels)     │
├─────────────────────────────────────┤
│         Domain Layer                 │
│        (Business Models)             │
├─────────────────────────────────────┤
│          Data Layer                  │
│  (Repository + Database + Prefs)    │
├─────────────────────────────────────┤
│         Service Layer                │
│      (WorkManager Workers)           │
└─────────────────────────────────────┘
```

### 模块说明

#### 1. UI Layer (`ui/`)

- **MainActivity**: 应用入口，处理权限请求和导航
- **SettingsScreen/ViewModel**: 设置页面，配置WiFi名称和扫描时间
- **LogScreen/ViewModel**: 日志页面，显示所有扫描记录
- **CalendarScreen/ViewModel**: 打卡页面，按月显示统计信息
- **NavGraph**: 导航配置

#### 2. Domain Layer (`domain/`)

- **DailyStats**: 每日统计数据模型

#### 3. Data Layer (`data/`)

- **Database**: Room数据库
  - `WifiScanRecord`: 扫描记录实体
  - `WifiScanDao`: 数据访问对象
  - `AppDatabase`: 数据库实例
- **Preferences**: `AppPreferences` 管理应用设置
- **Repository**: `WifiScanRepository` 封装数据访问逻辑

#### 4. Service Layer (`service/`)

- **WifiScanWorker**: WorkManager后台任务，执行WiFi扫描

#### 5. Util Layer (`util/`)

- **WifiScanner**: WiFi扫描工具类
- **WorkManagerHelper**: WorkManager任务调度辅助类

## 数据流

### WiFi扫描流程

```
WorkManager (每小时触发)
    ↓
WifiScanWorker.doWork()
    ↓
检查时间范围 → 检查WiFi状态 → WifiScanner.scanAndMatch()
    ↓
匹配到WiFi → WifiScanRepository.insertRecord()
    ↓
Room Database 保存记录
```

### UI数据流

```
ViewModel (StateFlow)
    ↓
Repository (Flow)
    ↓
DAO (Flow)
    ↓
Room Database
```

## 核心功能实现

### 1. WiFi扫描

- 使用 `WifiManager` 获取扫描结果
- 支持部分匹配（忽略大小写）
- 兼容不同Android版本的API差异

### 2. 定时任务

- 使用 `PeriodicWorkRequest` 每小时执行一次
- 在设置的时间范围内才执行扫描
- 使用 `OneTimeWorkRequest` 在时间边界触发

### 3. 数据存储

- Room数据库存储扫描记录
- SharedPreferences存储应用设置
- 使用Flow实现响应式数据流

### 4. UI展示

- Compose实现现代化UI
- 日历视图显示月度统计
- 超过3次匹配显示绿色标记

## 权限管理

应用需要以下权限：

- `ACCESS_WIFI_STATE`: 访问WiFi状态
- `CHANGE_WIFI_STATE`: 更改WiFi状态  
- `ACCESS_FINE_LOCATION`: 精确位置（Android 6.0+必需）
- `ACCESS_COARSE_LOCATION`: 粗略位置（Android 6.0+必需）
- `FOREGROUND_SERVICE`: 前台服务
- `POST_NOTIFICATIONS`: 通知（Android 13+）

权限在 `MainActivity` 中动态请求。

## 数据库设计

### WifiScanRecord 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| timestamp | Long | 扫描时间戳 |
| wifiName | String | 匹配到的WiFi全名 |
| matchedKeyword | String | 匹配的关键词 |

## 设置项

- `targetWifiName`: 目标WiFi名称（部分匹配）
- `scanStartHour`: 扫描开始时间（整点，0-23）
- `scanEndHour`: 扫描结束时间（整点，0-23）

## 注意事项

1. **Android版本兼容性**
   - Android 10+对WiFi扫描有更严格的限制
   - 可能需要用户手动触发扫描或启用位置服务

2. **后台任务限制**
   - WorkManager可能被系统限制
   - 建议引导用户关闭电池优化

3. **权限要求**
   - 位置权限是WiFi扫描的必要条件（Android 6.0+）
   - 需要在运行时动态请求

4. **数据统计**
   - 按日期聚合扫描记录
   - 计算每日匹配次数
   - 超过3次标记为成功（绿色）
