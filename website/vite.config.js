import { resolve } from 'path'
import { minifyHtml, injectHtml } from 'vite-plugin-html'
import { createHtmlPlugin } from 'vite-plugin-html'

import scalaVersion from './scala-version'

// https://vitejs.dev/config/
export default ({ mode }) => {
  const mainJS = `/target/scala-${scalaVersion}/website-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
  console.log('mainJS', mainJS)
  const script = `<script type="module" src="${mainJS}"></script>`

  return {
    server: {
      port: 6080,
    },
    base: '/v/0.17.x-calico/',
    publicDir: './src/main/public',
    build: {
      outDir: 'dist/v/0.17.x-calico',
    },
    plugins: [
      ...(mode === 'production' ? [
        minifyHtml(),
      ] : []),
      // injectHtml({
      //   injectData: {
      //     script
      //   }
      // })
      createHtmlPlugin({
        inject: {
          data: {
            script,
          },
        },
      }),

    ],
    resolve: {
      alias: {
        'stylesheets': resolve(__dirname, './src/main/static/stylesheets'),
        'svg': resolve(__dirname, './src/main/static/svg'),
      }
    }
  }
}
