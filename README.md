# 冒险人 · Adventurers

面向 **Minecraft 26.3 / Forge 66.0.9 / Java 25** 的文明演化模组。当前为 `0.1.0` 开发版本：从空仓库建立了可构建的模组、独立模拟核心和一条可玩的文明参与流程，**不是两份设计文档中全部远期功能的完成版**。

原始设计保存在 [CONTEXT.md](docs/design/CONTEXT.md) 和 [SYSTEMS.md](docs/design/SYSTEMS.md)。已实现、简化和未实现的部分逐项记录在 [实现范围](docs/IMPLEMENTATION.md)，架构见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)。

本机已通过 17 项核心验证和 4 项真实 Forge 服务器测试，详见 [验证记录](docs/VALIDATION.md)。

## 进入游戏

安装 Java 25、Minecraft 26.3 和 Forge **66.0.9**。把 `forge/build/libs/adventurers-0.1.0.jar` 放入客户端和服务器的 `mods` 目录。模组不依赖额外动画库或在线 AI 服务。

1. 进入世界，等待提示“文明已涌现”。生成阶段在服务器中分帧推进。
2. 输入 `/advent civilizations`，查看可以选择的文明。
3. 输入 `/advent join <文明编号> born`。也可选 `summoned`（被召唤）或 `transmigrated`（穿越）。身份只能领取一次，落点由模组选择。
4. 右键获得的**冒险人手记**查看指引，右键城邦居民交谈。手记也可用一本书和一个指南针无序合成。
5. `/advent city` 查看当地状况；`/advent requests` 询问真实需求；`/advent accept <编号>` 接取，携带物资后 `/advent deliver <编号>` 交付。物资会真正扣除并进入城邦，NPC 也可能先解决需求。
6. 在城邦里 `/advent meditate` 恢复魔力；靠近水源 `/advent observe water` 学习水元素，再尝试：

```text
/advent design tide heal 30 water:moon:magic:0
/advent cast tide
```

法术支持 `release`（视线内攻击）、`shield`、`heal`、`purify`。符文格式为 `元素:倾向:种类:层`，多个以逗号连接。例如 `fire:sun:magic:0`。编译器校验知识、同层冲突、能耗/载能与结构；非法法术不能记忆。日、月、混沌倾向分别为 `sun`、`moon`、`chaos`。失效施法不扣除魔力，正常施法有冷却。

常用指令：`/advent me`、`spells`、`history`、`banner true|false`。管理员另有 `status`、`pause true|false`、`simulate 1..24`、`save`、`selftest`；快进使用工作队列。普通玩家不能使用管理员命令。

配置文件由 Forge 在世界的 `serverconfig/adventurers-server.toml` 中生成。默认最多 4 个城邦、4096 个抽象 NPC，每个热区城邦显示至多 16 个村民外壳。建筑在已加载的城邦内按阶段呈现，仅填充空气/可替换植物；不会覆盖已有实心方块。

## 构建与验证

需要 **JDK 25**。Forge 的辅助工具还需要 **JDK 8**，它不是游戏运行时。Gradle Wrapper 固定为 9.7.1，并验证官方 SHA-256。

```bash
export JAVA_HOME=/path/to/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
# 两套 JDK 都安装后登记给 Gradle 与其 Mavenizer 子进程：
export JAVA_TOOL_OPTIONS="-Dorg.gradle.java.installations.paths=/path/to/jdk-8,/path/to/jdk-25"
./gradlew -Porg.gradle.java.installations.paths=/path/to/jdk-8,/path/to/jdk-25 :core:check :forge:build
./gradlew -Porg.gradle.java.installations.paths=/path/to/jdk-8,/path/to/jdk-25 :forge:runGameTestServer
./gradlew -Porg.gradle.java.installations.paths=/path/to/jdk-8,/path/to/jdk-25 :forge:runClient
```

GameTest 使用独立的 `forge/run-gametest` 测试世界。普通开发服务器使用 `:forge:runServer`；Minecraft EULA 由运行服务器的用户自行阅读与接受，安装脚本不会代为接受。

不下载 Minecraft 也能运行核心验证：

```bash
./scripts/test-core.sh
"$JAVA_HOME/bin/java" -cp core/build/offline dev.adventurers.core.cli.SimulationCli 42 720
"$JAVA_HOME/bin/java" -cp core/build/offline dev.adventurers.core.cli.PopulationBenchmark
# 或用 Gradle：
./gradlew -PcoreOnly :core:check :core:run --args='42 720'
```

Codex 云环境可执行 `scripts/setup-cloud.sh` 安装经过校验的工具链，再用 `python3 scripts/cloud-build.py :core:check :forge:build`。该脚本使用现有平台代理和系统 CA，不需要 GitHub Token。

## 协作与发布

开发改动通过独立分支与 Pull Request 交接，交付约定见 [AGENTS.md](AGENTS.md)。GitHub Actions 在推送与 PR 上执行构建和测试；推送 `v*` 版本标签时，只有同次构建和测试成功，才会上传 JAR、`SHA256SUMS` 并发布 GitHub 预发布版本。发布前须同步项目版本与 `docs/releases/<版本>.md`。已发布版本不会被流程覆盖。

## 存档

模拟数据位于 `<世界>/data/adventurers/world.bin`，每分钟和正常停服时保存，保留上一份 `.bak`。格式带版本、长度边界和 CRC；写入采用临时文件加原子替换。发现损坏会停止读写，不会用空世界覆盖存档。恢复备份前应先复制保留故障文件。

建筑呈现进度单独保存在同目录的 `projection.properties`；应与整个 Minecraft 世界一起备份。服务器进程、实体句柄和热区状态不会写入核心存档，重启后根据玩家位置重建。

许可证：仓库原有的 [GPL-3.0](LICENSE)。
