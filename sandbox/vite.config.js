import { resolve } from 'path'
// import { minifyHtml, injectHtml } from 'vite-plugin-html'
import scalaVersion from './scala-version'
import { createHtmlPlugin } from 'vite-plugin-html'

// https://vitejs.dev/config/
export default ({ mode }) => {
  const mainJS = `/target/scala-${scalaVersion}/sandbox-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
  console.log('mainJS', mainJS)
  const script = `<script type="module" src="${mainJS}"></script>`

  return {
    server: {
      port: 6080,
    },
    publicDir: './src/main/public',
    plugins: [
      // ...(process.env.NODE_ENV === 'production' ? [
      //   minifyHtml(),
      // ] : []),
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

    ]
  }
}