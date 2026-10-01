import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier/flat'

/**
 * ESLint 9/10 flat config（前端专用）
 *
 * 设计取舍：
 * 1. 只继承 `plugin:vue/vue3-essential`（即 flat/essential），**不用** vue3-recommended。
 *    recommended 会一次报出上千条风格问题，把真正的错误淹没掉。
 * 2. 用 `eslint-config-prettier/flat` 关掉与 Prettier 冲突的格式类规则；
 *    格式交给 Prettier CLI（`npm run format`），不让 ESLint 兼任格式化。
 *    因此这里**不**启用 `prettier/prettier` 规则，避免 lint 输出被格式噪音刷屏。
 * 3. 下面 `project/rules` 里的几条是 docs/AI-CONTEXT.md §七 的红线，必须机器可执行。
 */
export default [
  {
    name: 'project/ignores',
    ignores: ['dist/**', 'node_modules/**', 'src/assets/**', 'public/**']
  },

  ...pluginVue.configs['flat/essential'],

  prettierConfig,

  {
    name: 'project/rules',
    rules: {
      // —— 红线（AI-CONTEXT.md §七）——
      // 项目当前零 v-html，这是最重要的护城河：一旦有人引入 <div v-html="..."> 立刻拦下。
      'vue/no-v-html': 'error',
      // 禁止直接改写 props（Vue 单向数据流）。
      'vue/no-mutating-props': 'error',
      // 允许 console.error / console.warn：项目刻意保留的可观测性，不要一棍子打死。
      'no-console': ['warn', { allow: ['error', 'warn'] }],
      // 未使用的变量/import —— error 级，提交前必须清理。
      'no-unused-vars': [
        'error',
        {
          args: 'after-used',
          ignoreRestSiblings: true,
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_'
        }
      ],
      // 项目里存在单单词组件名（如 App.vue），先关掉避免噪音。
      'vue/multi-word-component-names': 'off'
    }
  }
]
