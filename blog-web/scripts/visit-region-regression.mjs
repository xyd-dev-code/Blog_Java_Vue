import assert from 'node:assert/strict'
import { formatProvince } from '../src/utils/visitRegion.js'

for (const [input, expected] of [
  ['Beijing', '北京'], [' BEIJING ', '北京'], ['Beijing City', '北京'],
  ['Shanghai', '上海'], ['Guangdong Province', '广东'],
  ['Shanxi', '山西'], ['Shaanxi', '陕西'], ['Inner Mongolia', '内蒙古'],
  ['Hong Kong', '香港'], ['北京市', '北京市'], ['抚顺市', '抚顺市'],
  ['United States', 'United States'], ['日本', '日本'], ['Berlin', 'Berlin'],
  ['United States · California · Los Angeles', 'United States · California · Los Angeles'],
  ['美国 · 加利福尼亚州 · 洛杉矶', '美国 · 加利福尼亚州 · 洛杉矶'],
  ['France · Paris', 'France · Paris'],
  ['', '未知'], ['  ', '未知'], [null, '未知'], [undefined, '未知'], ['0', '未知'],
]) {
  assert.equal(formatProvince(input), expected, `地区显示错误: ${input}`)
}
console.log('Visit region regression passed (22 cases).')
