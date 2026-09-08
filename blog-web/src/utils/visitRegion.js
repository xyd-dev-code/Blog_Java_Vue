// 历史访问日志可能包含英文地区名；仅规范显示，不改变筛选时使用的原始值。
const regionAliases = new Map(Object.entries({
  beijing: '北京', shanghai: '上海', tianjin: '天津', chongqing: '重庆',
  hebei: '河北', shanxi: '山西', liaoning: '辽宁', jilin: '吉林',
  heilongjiang: '黑龙江', jiangsu: '江苏', zhejiang: '浙江', anhui: '安徽',
  fujian: '福建', jiangxi: '江西', shandong: '山东', henan: '河南',
  hubei: '湖北', hunan: '湖南', guangdong: '广东', hainan: '海南',
  sichuan: '四川', guizhou: '贵州', yunnan: '云南', shaanxi: '陕西',
  gansu: '甘肃', qinghai: '青海', taiwan: '台湾',
  'inner mongolia': '内蒙古', neimenggu: '内蒙古', guangxi: '广西',
  tibet: '西藏', xizang: '西藏', ningxia: '宁夏', xinjiang: '新疆',
  'hong kong': '香港', hongkong: '香港', macau: '澳门', macao: '澳门',
  china: '中国', cn: '中国',
}))

export function formatProvince(value) {
  const name = typeof value === 'string' ? value.trim() : ''
  if (!name || name === '0' || name === '未知') return '未知'
  const key = name.toLowerCase().replace(/\s+/g, ' ').replace(/\s+(province|city|shi|sheng)$/, '')
  // 名称的语言不代表国家；没有国家字段时，不推断是否境外。
  return regionAliases.get(key) || name
}
