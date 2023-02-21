import {resolve} from 'path'
import {createHtmlPlugin} from 'vite-plugin-html'
import commonjs from '@rollup/plugin-commonjs';

import scalaVersion from './scala-version'

// https://vitejs.dev/config/
export default ({mode}) => {
  const mainJS = `target/scala-${scalaVersion}/website-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
  const script = `<script type="module" src="${mainJS}"></script>`

  /** @type {import('vite').UserConfig} */
  return {
    server: {
      port: 6080,
    },
    base: '/v/0.17.x-calico/',
    publicDir: './src/main/public',
    build: {
      outDir: 'dist/v/0.17.x-calico',
    },
    optimizeDeps: {
      disabled: true,
    },
    plugins: [
      ...(mode === 'production' ? [commonjs()] : []),
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
