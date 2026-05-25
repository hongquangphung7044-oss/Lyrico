import { defineConfig } from 'vitepress'
import { GitChangelog, GitChangelogMarkdownSection } from '@nolebase/vitepress-plugin-git-changelog'

const base = '/Lyrico/'

export default defineConfig({
  title: 'Lyrico 文档',
  description: 'Lyrico 使用说明与插件开发文档',
  lang: 'zh-CN',
  base,

  head: [
    ['link', { rel: 'icon', href: `/Lyrico/logo.svg`, type: 'image/svg+xml' }],
  ],

  vite: {
    plugins: [
      GitChangelog({
        repoURL: () => 'https://github.com/Replica0110/Lyrico',
      }),
      GitChangelogMarkdownSection(),
    ],
  },

  themeConfig: {
    logo: `/logo.svg`,

    nav: [
      { text: '首页', link: '/' },
      { text: '插件', link: '/plugins/' }
    ],

    sidebar: {
      '/plugins/': [
        {
          text: '使用插件',
          items: [
            { text: '插件首页', link: '/plugins/' },
            { text: '使用插件', link: '/plugins/using' }
          ]
        },
        {
          text: '开发插件',
          items: [
            { text: '从零编写插件', link: '/plugins/examples' },
            { text: '本地调试插件', link: '/plugins/debugging' },
            { text: '插件包结构', link: '/plugins/composition' },
            { text: '插件函数', link: '/plugins/plugin-functions' },
            { text: '配置项与元数据', link: '/plugins/config-metadata' }
          ]
        },
        {
          text: '参考手册',
          items: [
            { text: 'Manifest 参考', link: '/plugins/manifest' },
            { text: '宿主 API 参考', link: '/plugins/host-api' }
          ]
        },
        {
          text: '运行机制',
          items: [
            { text: '架构与生命周期', link: '/plugins/overview' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Replica0110/Lyrico' }
    ],

    outline: {
      level: [2, 3],
      label: '本页目录'
    },

    docFooter: {
      prev: '上一页',
      next: '下一页'
    },

    lastUpdated: {
      text: '最后更新'
    }
  }
})
