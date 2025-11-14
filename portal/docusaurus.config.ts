import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type * as Plugin from "@docusaurus/types/src/plugin";
import type * as OpenApiPlugin from "docusaurus-plugin-openapi-docs";
import path from 'path';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'Spec-Coding Modulith',
  tagline: 'A showcase project of Spring Modulith with Spec Driven Development.',
  favicon: 'img/favicon.ico',

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://akiohoshikawa.github.io/',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/spec-coding-modulith/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'example', // Usually your GitHub org/user name.
  projectName: 'spec-coding-modulith', // Usually your repo name.

  onBrokenLinks: 'throw',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          docItemComponent: "@theme/ApiItem", // Derived from docusaurus-theme-openapi
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],
  plugins: [
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'overview',
        path: path.resolve(__dirname, '../doc/overview'),
        include: ['**/*.{md,mdx}'],
        routeBasePath: 'docs/overview',
        sidebarPath: false,
      },
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'business-rule',
        path: path.resolve(__dirname, '../doc/business-rule'),
        include: ['**/*.{md,mdx}'],
        exclude: ['**/*_TEMPLATE.md'],
        routeBasePath: 'docs/business-rule',
      },
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'user-story',
        path: path.resolve(__dirname, '../doc/user-story'),
        include: ['**/*.{md,mdx}'],
        exclude: ['**/*_TEMPLATE.md'],
        routeBasePath: 'docs/user-story',
      },
    ],
    [
      'docusaurus-plugin-openapi-docs',
      {
        id: "api",
        docsPluginId: "classic",
        config: {
          openapi: {
            specPath: "../doc/api/openapi-bundled.yaml",
            outputDir: "docs/openapi",
            sidebarOptions: {
              groupPathsBy: "tag",
            },
          } satisfies OpenApiPlugin.Options,
        }
      },
    ],
  ],

  themes: ["docusaurus-theme-openapi-docs"],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Spec-Coding Modulith',
      logo: {
        alt: 'My Site Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          to: 'docs/overview/project-overview',
          label: 'Overview',
        },
        {
          to: 'docs/business-rule',
          label: 'Business Rule',
        },
        {
          to: 'docs/user-story',
          label: 'User Story',
        },
        {
          type: 'docSidebar',
          sidebarId: 'openApiSidebar',
          position: 'left',
          label: 'OpenAPI',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [],
      copyright: `Copyright © ${new Date().getFullYear()} Spec Driven Modulith. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
