const scalaVersion = require('./scala-version')

module.exports = (api) => {
  const scalajsMode = api.mode === 'production' ? 'opt' : 'fastopt'
  return {
    content: [
      `./target/scala-${scalaVersion}/website-${scalajsMode}/*.js`,
      './src/main/static/html/*',
    ],
    theme: {
      extend: {
        fontFamily: {
          display: ['Oxanium', 'ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'Times', 'serif'],
          serif: ['Inter', 'ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'Times', 'serif'],
          mono: [
            'JetBrains Mono',
            'ui-monospace',
            'SFMono-Regular',
            'Menlo',
            'Monaco',
            'Consolas',
            '"Liberation Mono"',
            '"Courier New"',
            'monospace',
          ],
        }
      },
    },
    variants: {
      textColor: ['responsive', 'hover', 'focus', 'group-hover'],
      opacity: ['responsive', 'hover', 'focus', 'group-hover', 'disabled'],
      textOpacity: ['responsive', 'hover', 'focus', 'group-hover', 'disabled'],
      cursor: ['responsive', 'hover', 'focus', 'group-hover', 'disabled'],
      animations: ['responsive'],
      animationDuration: ['responsive'],
      animationTimingFunction: ['responsive'],
      animationDelay: ['responsive'],
      animationIterationCount: ['responsive'],
      animationDirection: ['responsive'],
      animationFillMode: ['responsive'],
      animationPlayState: ['responsive'],
    },
    corePlugins: {},
    plugins: [
      require('@tailwindcss/typography'),
      require('@tailwindcss/forms'),
    ],
  }
}
