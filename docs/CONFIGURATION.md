# 食物配置

新建 `config/baiweiqixiao/food_overrides.json`，使用与内置映射相同的结构。示例见 [food_overrides.example.json](food_overrides.example.json)。

`foods` 的键是完整物品ID。`added` 中每个效果独立设置 `effect`、`level`、`ticks` 和 `probability`；持续时间单位是游戏刻，正常速度下20刻为1秒，概率范围为0至1。原创效果使用 `original:01` 至 `original:21`，对应顺序见源码 `Effects.IDS`。

`native` 按状态效果ID处理原有效果。`remove: true` 阻止该食品施加指定效果，不能用来清除玩家已经拥有的效果。保留的效果可指定等级、时间，或沿用默认值。

一条食品覆盖记录会整体替换原记录，空 `added` 表示该食品不再添加新效果。改完后重启实例；联机时请让客户端和服务端使用相同配置。错误配置会阻止启动，请保留修改前的副本。

默认映射在 `src/main/resources/data/baiweiqixiao/food_effects.json`。本模组不会根据本次使用的配方动态继承效果。
