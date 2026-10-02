# 当前验证记录

验证日期：2026-10-02。平台：Linux x86_64，Temurin JDK 25.0.4.1+1，辅助 JDK 8u504-b01，Gradle 9.7.1，Minecraft 26.3，Forge 66.0.9。

| 检查 | 结果 |
|---|---|
| `scripts/setup-cloud.sh` | 完整构建成功 |
| 移开本次手动创建的全局 Gradle 配置后再次执行安装脚本 | 成功，脚本自身包含必要代理、CA 与 JDK 初始化 |
| `:core:check` / 自定义 `simulationTest` | 17 通过、0 失败、0 跳过 |
| `:forge:build` | 成功，模组 JAR 含核心类、注册、配方和资源包 |
| `:forge:runGameTestServer --tests adventurers:*`（选择器已配置在 Gradle 中） | 4 个 Adventurers 测试全部通过 |
| JSON 资源解析、JAR 必要文件检查、Git diff 空白检查 | 通过 |

核心报告：`core/build/test-results/simulation/TEST-simulation.xml`。独立断言套件使用 `validation` 源集，由 `check` 和 `test` 任务强制执行并输出 XML；默认 JUnit `test` 任务显示 `NO-SOURCE` 是正常现象，不能把它当作验证证据。实际验证证据是 17 项断言的结果和退出码。

Forge 报告：`forge/run-gametest/gametest-results.xml`，已确认本次报告包含以下四个测试，而不是原版空测试：

- `adventurers:runtime`：生命周期初始化、手记注册、实际 `/advent selftest` 命令。
- `adventurers:save_replay`：文明生成后保存/恢复，两条路径继续运行六日，完整存档字节一致。
- `adventurers:player_magic`：服务器中的真实玩家实体得到治疗，魔力扣除，立即重复施法被冷却拒绝。
- `adventurers:quests_and_projection`：玩家背包物品被扣除、城邦库存增加、重复交付被拒绝；热区产生有限 NPC 外壳，多次呈现不重复，冷区移除外壳。

一次独立压力测量：100,000 个冷区抽象 NPC 推进一个模拟日约 **418.0 ms**，存档 **16.76 MiB**，读取后人口数量一致。这是单次测量，不是帧率保证；一个系统内仍需要进一步分片，不能据此声称 10 万可见 NPC 或稳定 20 TPS。

产物：`forge/build/libs/adventurers-0.1.0.jar`，198349 字节。本次 SHA-256：

```text
65ab62f71f73de6376b1adc2337259d852d868b92bb7180165f49e2fe56c8d49
```

本记录覆盖本地云环境验证。未执行：交互客户端画面检查、长期多人压力测试、其他操作系统验证。GitHub Actions 的远端结果见仓库 Actions 与关联 PR；可下载版本及其状态见 GitHub Releases。未发布到外部模组平台。具体设计缺口见 [IMPLEMENTATION.md](IMPLEMENTATION.md)。
