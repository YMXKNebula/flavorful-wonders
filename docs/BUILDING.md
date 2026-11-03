# 从源码构建

需要JDK 25、Python 3和一个已经安装Minecraft 26.3、Fabric Loader、Fabric API、Farmer’s Delight Refabricated及AppleSkin的本地实例。AppleSkin在游戏运行时可选，但编译兼容代码时需要它的API。

Minecraft 26.3使用可直接引用的命名字节码。准备脚本只读取你自己的实例和依赖，不会改动实例或存档，也不会下载或分发游戏文件。

先运行：

```text
python tools/configure_build.py --instance /path/to/versions/your-instance --libraries /path/to/.minecraft/libraries
```

脚本会从实例版本JSON读取游戏库，从Fabric Loader安装描述读取配套库，并提取Fabric API和Loader内嵌的编译依赖。路径仅写入被Git忽略的 `.local/classpath.json`。

设置 `JAVA_HOME` 指向JDK 25，然后运行：

```text
# Windows
gradlew.bat build mechanismTest

# Linux / macOS
./gradlew build mechanismTest
```

首次使用Wrapper需要联网下载Gradle 9.4.1。成品位于 `build/libs/flavorful-wonders-26.3-1.0.0.jar`。`src/gametest`、`src/clienttest` 保留游戏测试源码；普通构建只编译主源码和纯Java结算测试，不会把游戏测试夹具装进发行JAR。
