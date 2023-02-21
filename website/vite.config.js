import {defineConfig} from 'vite'
import {resolve} from 'path'
import {createHtmlPlugin} from 'vite-plugin-html'
// import commonjs from '@rollup/plugin-commonjs';
const fs = require('fs');

import scalaVersion from './scala-version'

// https://vitejs.dev/config/
export default ({mode}) => {
  const mainJS = `target/scala-${scalaVersion}/website-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
  const moduleJS = `module.js`
  console.log('mainJS', mainJS)
  fs.writeFileSync(moduleJS, `import './${mainJS}'`)

  const script = `<script type="module" src="${mainJS}"></script>`
  // const script = `<script type="module" src="/${moduleJS}"></script>`

  /** @type {import('vite').UserConfig} */
  return {
    server: {
      port: 6080,
    },
    base: '/v/0.17.x-calico/',
    publicDir: './src/main/public',
    build: {
      outDir: 'dist/v/0.17.x-calico',
      commonjsOptions: {
        // include: [mainJS],
        // include: [mainJS, /node-modules/],
        // include: [mainJS, /highlight.js/, /marked/],
        // transformMixedEsModules: true,
        // strictRequires: 'debug',
        strictRequires: true,
      }
    },
    optimizeDeps: {
      disabled: false,
      force: true,
      include: [
        // moduleJS,
        mainJS,
        // 'highlight.js',
        // 'marked',
        // 'marked.js',
        // moduleJS,
      ]
    },
    plugins: [
      // commonjs({
      // }),
      createHtmlPlugin({
        minify: mode === 'production',
        inject: {
          data: {
            script,
            pl: mode === 'production' ? '<script async defer data-domain="frontroute.dev" src="/js/pl-script.js"></script>' : ''
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
