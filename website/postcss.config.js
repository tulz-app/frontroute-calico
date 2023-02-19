module.exports = (api) => {
  const tailwindcss = require('./tailwind.config')(api)
  const plugins = {
    'tailwindcss/nesting': {},
    'postcss-import': {},
    tailwindcss,
    autoprefixer: {}
  }
  if (api.mode === 'production') {
    plugins.cssnano = {}
  }

  console.log('postcss', plugins)
  return {
    plugins
  }
}
