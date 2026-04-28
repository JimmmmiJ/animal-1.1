import { mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'

const routes = [
  '/health-data',
  '/breeding',
  '/disease',
  '/behavior',
  '/dashboard',
  '/alerts',
  '/settings'
]

const baseUrl = process.env.MANUAL_BASE_URL || 'http://localhost:4173'
const outputDir = resolve(process.cwd(), process.env.MANUAL_SCREENSHOT_DIR || '../manual_check/latest')

async function main() {
  let chromium
  try {
    ;({ chromium } = await import('playwright'))
  } catch {
    throw new Error('缺少 Playwright。请先安装依赖，或在已有 playwright 的环境中运行。')
  }

  await mkdir(outputDir, { recursive: true })
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 })

  for (const route of routes) {
    await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' })
    const filename = route === '/' ? 'home' : route.slice(1).replace(/\//g, '-')
    await page.screenshot({ path: resolve(outputDir, `${filename}.png`), fullPage: true })
  }

  await browser.close()
  console.log(`Manual screenshots saved to ${outputDir}`)
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
