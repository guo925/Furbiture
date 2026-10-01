/**
 * ECharts 按需引入（全项目唯一的注册点）
 *
 * <p><b>为什么要按需：</b>三个视图此前各自写 `import * as echarts from 'echarts'`，
 * 把整个 ECharts 打进了产物——echarts chunk 实测 1,125 kB（gzip 381 kB），
 * 而项目真正用到的只有折线图、饼图和三个基础组件。
 *
 * <p><b>为什么集中在这一个文件：</b>`echarts.use([...])` 是**全局注册**，
 * 一旦「注册项」和「初始化图表的代码」分散在不同文件里，漏注册一个组件**不会报错**，
 * 只会在运行时静默少画一部分（典型表现：图例不显示、提示框不出来，但图表本身正常）。
 * 收敛到唯一一处后，新增图表类型只需要改这里。
 *
 * <p><b>新增图表时的自查清单：</b>
 * <ol>
 *   <li>图表类型（`type: 'bar'`）→ 从 `echarts/charts` 引入并注册</li>
 *   <li> `legend` / `tooltip` / `title` / `dataZoom` / `visualMap` 等配置项
 *        → 各自对应一个 Component，从 `echarts/components` 引入并注册</li>
 *   <li>渲染方式 → 默认 Canvas；需要矢量输出再加 `SVGRenderer`</li>
 * </ol>
 *
 * <p>ECharts 6 保留了与 5.x 相同的子路径（core / charts / components / renderers）。
 * 从 `echarts/core` 导入后，`init()` 与 `graphic.LinearGradient` 都可直接使用。
 */
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart, // type: 'line' —— 销售趋势 / 营收趋势
  PieChart, // type: 'pie'  —— 品类分布
  GridComponent, // 直角坐标系底板，折线图必需
  TooltipComponent, // 悬停提示（三个图表都用）
  LegendComponent, // 饼图图例
  CanvasRenderer // 渲染器
])

export default echarts
