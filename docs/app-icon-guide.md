# 应用图标创建指南

## 图标要求

应用需要以下尺寸的图标：

### 标准图标 (ic_launcher)
- **mdpi**: 48x48 px
- **hdpi**: 72x72 px
- **xhdpi**: 96x96 px
- **xxhdpi**: 144x144 px
- **xxxhdpi**: 192x192 px

### 圆形图标 (ic_launcher_round)
- **mdpi**: 48x48 px
- **hdpi**: 72x72 px
- **xhdpi**: 96x96 px
- **xxhdpi**: 144x144 px
- **xxxhdpi**: 192x192 px

## 图标设计建议

1. **主题**: WiFi/网络相关
2. **颜色**: 使用Material Design调色板
3. **风格**: 简洁、现代、易于识别
4. **背景**: 可以使用品牌色或透明背景

## 创建步骤

### 方法1: 使用Android Studio

1. 右键点击 `app/src/main/res` 目录
2. 选择 `New` > `Image Asset`
3. 选择 `Launcher Icons (Adaptive and Legacy)`
4. 设计图标
5. 点击 `Next` 和 `Finish`

### 方法2: 使用在线工具

推荐工具：
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)
- [App Icon Generator](https://www.appicon.co/)

### 方法3: 手动创建

1. 设计主图标（建议1024x1024px）
2. 使用工具生成各尺寸版本
3. 放置到对应目录：
   - `app/src/main/res/mipmap-mdpi/ic_launcher.png`
   - `app/src/main/res/mipmap-hdpi/ic_launcher.png`
   - `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
   - `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
   - `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

## 临时解决方案

在开发阶段，可以使用Android Studio自动生成的默认图标。应用仍然可以正常运行，只是使用系统默认图标。

## 图标资源目录结构

```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.png (48x48)
│   └── ic_launcher_round.png (48x48)
├── mipmap-hdpi/
│   ├── ic_launcher.png (72x72)
│   └── ic_launcher_round.png (72x72)
├── mipmap-xhdpi/
│   ├── ic_launcher.png (96x96)
│   └── ic_launcher_round.png (96x96)
├── mipmap-xxhdpi/
│   ├── ic_launcher.png (144x144)
│   └── ic_launcher_round.png (144x144)
└── mipmap-xxxhdpi/
    ├── ic_launcher.png (192x192)
    └── ic_launcher_round.png (192x192)
```

## 注意事项

1. 图标文件名必须与AndroidManifest.xml中配置的一致
2. 圆形图标是可选的，但建议提供以获得更好的用户体验
3. 确保图标在不同背景下都清晰可见
4. 遵循Material Design图标设计指南
