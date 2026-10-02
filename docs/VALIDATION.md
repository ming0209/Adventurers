# 0.2.1 验证记录

验证日期：2026-10-02。平台：Linux x86_64，Temurin JDK 25.0.4.1+1，辅助 JDK 8u504-b01，Gradle 9.7.1，Minecraft 26.3，Forge 66.0.9。

| 检查 | 最终结果 |
|---|---|
| `scripts/test-core.sh`（不依赖 Minecraft） | 25 项通过 |
| `:core:check` / 自定义 `simulationTest` | 25 通过、0 失败、0 跳过 |
| `:forge:build` | 成功 |
| `:forge:runGameTestServer` | 普通测试世界中 8 项通过 |
| `-PplanetTest :forge:runGameTestServer` | 真实星球预设中 8 项通过 |
| JSON 资源解析、JAR 内容/版本检查、`git diff --check` | 通过 |

云环境沿用 0.1.0 已验证的 `scripts/setup-cloud.sh` 工具链；本次使用 `python3 scripts/cloud-build.py <任务> --no-daemon` 构建。没有代替用户接受普通服务器的 Minecraft EULA。

## 核心检查

报告：`core/build/test-results/simulation/TEST-simulation.xml`。独立断言套件使用 `validation` 源集，`check` 和 `test` 强制执行并输出 XML。默认 JUnit `test` 显示 `NO-SOURCE` 不能当作测试通过的证据，实际证据为 25 项断言及退出码。

原有 17 项覆盖调度预算、自然文明涌现、资源事务、法术编译、任务权限与时限、存档一致性和长期模拟；新增 8 项覆盖：

- 东西和两极跨界、多圈归一化、非法坐标拒绝。
- 相同种子复现、不同种子差异、陆海/河网与气候多样性。
- 经界及极点两侧地形和洞穴一致性。
- 多线程反序采样不改变生成结果。
- 干燥、可建文明落点及区域坐标一致性。
- 三种星球大小、陆地出生搜索与不支持设置的拒绝。
- 种子 42 / 77 / 2026 的文明生成、存档重载后继续 24 日的完整字节一致。
- 真实已发布格式 1 样本迁移：身份、科技/魔法、城邦坐标保留，原文件备份不变。

旧存档样本来源、内容及校验值见 `core/src/validation/resources/README.md`，不是使用当前编码器伪造的旧版本头。

## Forge 服务器检查

报告分别位于 `forge/run-gametest/gametest-results.xml` 和 `forge/run-planet-gametest/gametest-results.xml`。两份报告都包含下面 8 项测试，无失败或跳过。`runtime` 断言实际生成器与模拟地理类型，防止星球数据包未加载仍被误判为通过。

| 测试 | 检查内容 |
|---|---|
| `adventurers:runtime` | 生命周期、注册、预设类型、实际 `/advent selftest` |
| `adventurers:save_replay` | 文明存档恢复后继续六日，完整字节一致 |
| `adventurers:player_magic` | 真实玩家治疗、魔力扣除和重复施法冷却 |
| `adventurers:quests_and_projection` | 真实背包交付、库存增加、重复拒绝；区块实体就绪后检查各热区城邦人数上限、不重复和冷区清理 |
| `adventurers:planet_presets` | 三份真实 JSON 预设解码、尺寸、生成器编解码、种子差异 |
| `adventurers:planet_terrain` | 相邻真实 ProtoChunk 的地表、水面、基岩、高度图与重建一致；星球服务器上用原版出生搜索找到干燥落点 |
| `adventurers:planet_boundary` | 真正的储物船携带乘客和 7 个铁锭，东西/极点传送保留骑乘、货物、速度与朝向 |
| `adventurers:planet_atlas` | 真实地图像素、锁定、单 ID 复用、灭绝标记更新、服务重建后索引保持 |

初次远端验证中，`v0.2.0` 的星球居民测试失败，发布被阻止。模拟客户端不发送移动数据，`snapTo` 不会建立玩家的区块加载票据；仅等待实体出现不足以修复。测试现显式激活城邦区块，再等待实体区域就绪，按每个热区城邦计算上限。实际居民生成同样检查实体区域已加载并运行，避免在尚不可见的区域重复创建外壳；落点使用碰撞体检查，允许草丛、拒绝头顶树叶。两种世界均包含草丛/头部碰撞的回归检查。未删除或跳过失败测试。修复版采用新版本 0.2.1，不覆盖已有标签。

`src/gametest/packs/planet` 仅供测试，覆盖 GameTest 固定使用的世界预设；已验证发行 JAR 不含该覆盖，也不含旧档样本。所有测试世界保持 Git 忽略。

远端进一步验证了 Minecraft 的实体加载前置条件：每个实体区块需要两圈已生成邻区。测试先完成城邦及邻区的加载，再开始按 tick 等待实体就绪，避免 GameTest 无实时限速的时钟在较慢机器上先耗尽断言预算；断言预算仍为 300 tick。最终星球回归还使用 `-XX:ActiveProcessorCount=2` 限制 JVM 处理器数量，8 项全部通过。

## 产物与边界

本地模组：`forge/build/libs/adventurers-0.2.1.jar`，257424 字节，SHA-256：

```text
c1cac50845e5d546fff130e9f010cbd5e8e56972e0fccb07c61e08fa4b2d5c69
```

GitHub Actions 对分支、PR、版本标签重新构建，发布任务仅在标签构建及两种服务器测试成功后运行。Release 附带 JAR 与 `SHA256SUMS`，远端运行记录可在 Actions/PR 查看。

未执行交互客户端画面检查、长期多人压力和其他操作系统验证；未测量本版的大规模区块生成或多人 TPS。核心并发采样通过不等同于上述性能保证。地图数据/传送测试也不等同于已完成视觉无缝环绕。具体缺口见 [IMPLEMENTATION.md](IMPLEMENTATION.md)。
