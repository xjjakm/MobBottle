# Changelog

All notable changes to the MobBottle mod are documented in this file.

## [26.2-2] - 2026-09-04

### 新增

- 新增简体中文语言文件（zh_cn.json），补全物品名、提示与 Mod Menu 翻译

- 新增 Mod Menu 中文/日文/英文描述与简介（modmenu.descriptionTranslation / summaryTranslation）

- 新增模组图标（assets/mobbottle/icon.png），并注册至 fabric.mod.json

- 新增专属创造模式物品栏标签页「生物展示瓶」，与「工具与实用工具」页并存

- 方块加入 `minecraft:mineable/pickaxe` 标签，镐子可加速挖掘（空手时间不变）

### 调整

- 物品名称更新：生物瓶 → 生物展示瓶（中文）、Empty Mob Bottle → 空生物展示瓶

- 手持瓶子的第一人称显示变换：向远离镜头方向偏移（z=-2.0、比例 0.3、旋转 45°），不再贴脸

- 空瓶（mob_bottle_empty）添加说明文字「只是个装饰物而已，不能装生物」

- 主合成配方自动解锁条件放宽：获得木台阶 / 玻璃 / 宝石（紫水晶碎片，钻石，绿宝石，青金石，海晶碎片，下界石英）任一材料即解锁，不用在不知道配方的情况下下合成

## [26.2-1] - 2026-09-03

### 移植

- 从 NeoForge 移植至 Fabric（Minecraft 26.2 + Fabric Loader 0.19.5 + Fabric API 0.159.0）

- 配方 / 战利品表改为手写数据包 JSON（适配 26.2 新 ingredient 格式）

- 移除 Neoforge 模板与生成文件，配置 build.gradle / fabric.mod.json / 生成模板

### 修复

- 通过 Fabric UseEntityCallback 恢复「右键生物捕获」交互链路

- 修复客户端渲染专用实体未分配 Entity ID 导致的崩溃（分配负值假 ID）

- 修复特效拾取动画场景下空组件物品的 NPE 崩溃

- 修复放置后瓶内生物抖动 / 抽搐（冻结渲染实体的 ageInTicks，并清空瓶内阴影）

- 修复物品栏中瓶子亮度随地图原点所处时段 / 天气闪动的问题

- 修复特殊物品渲染偶发整瓶消失（抽取失败时兜底渲染）

- 修复物品栏中瓶子因距离剔除不可见的问题（shouldRender 恒真）