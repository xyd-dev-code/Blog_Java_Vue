# 天气 IP 定位

首页天气不再调用浏览器 Geolocation API，打开首页、重试、恢复自动定位都不会请求位置授权。

- 默认：可选后端区县服务 → 浏览器直连 IPWhois → IPAPI.is 经纬度备用 → ipapi.co 城市备用 → 提示手动选择城市。
- IPAPI.is 匿名接口只返回估算经纬度时，显示“附近”并获取天气，不编造城市或区县名。
- 手动搜索城市或区县后，选择保存在当前浏览器；“自动”清除选择，重新识别 IP。
- IP 结果只做当前标签页的 10 分钟缓存，不保存原始 IP。禁用浏览器存储时功能仍可使用。
- 定位失败、天气失败、城市搜索失败分别提示；不会因拒绝设备权限而阻止天气加载。
- 天气标题仅显示地点名称，不追加“IP 估算”；估算性质保留在悬停说明和城市选择面板中。VPN、代理、移动网络等可能使归属地偏离实际位置。

## 区县精度

免密钥的 IPWhois 接口返回城市与大致经纬度，没有独立的区县字段。默认无需额外配置，但不能承诺每位访客都精确到区。

已接入 IP2Location.io 的可选服务端查询。部署后端时设置环境变量 `BLOG_WEATHER_IP2LOCATION_KEY`，使用拥有 `district` 字段权限的套餐。未配置、套餐不支持、查询失败或本地/内网请求会自动降级。Key 只留在服务端，既不进入前端构建，也不通过站点公开配置返回。没有自动注册账号、开通付费服务或填入任何凭证。

当服务明确返回区县时显示“城市 · 区县”；没有区县时仅显示城市，不根据城市中心坐标反推出一个区名。上游经纬度本身也可能是城市代表点，因此区县名称不等于设备坐标或区县级精确天气。要求实际所在区的准确天气时，访客可搜索并选择对应区县；搜索覆盖以 Open-Meteo 地名库为准。

## 部署与隐私

可选接口为 `GET /api/v1/site/weather-location`，无需登录，响应 `Cache-Control: no-store`，避免 CDN 把上一位访客的位置复用给下一位。服务端只缓存最多 2000 个 IP 的查询结果，10 分钟过期，不写数据库。

该接口复用项目的 `ClientIpResolver`：只信任同机反代追加的来源信息。Nginx 应覆盖 `X-Real-IP` 并正确追加 `X-Forwarded-For`；如增加 CDN 或异机代理，需要先按实际可信代理链配置真实访客 IP，不能直接信任任意客户端提交的转发头。开发环境的回环/内网 IP 不查询，也不会使用网站服务器自己的公网位置；浏览器自动走直连接口。

浏览器会连接 IPWhois（备用 IPAPI.is、ipapi.co）查询自身网络出口，并把估算位置或手动选择的经纬度传给 Open-Meteo；可选区县服务会把访客 IP 发给 IP2Location.io。定位请求省略 Cookie 和 Referer。生产站点应在隐私说明中披露这些用途，并根据访问量与服务条款配置额度；公共接口存在限流和可用性限制。IPAPI.is 匿名接口按每个客户端 IP 每日 100 次限额使用，失败时不自动反复重试。

## 验证

- 前端构建：在 `blog-web` 运行 `npm run build`。
- 浏览器回归：启动前端后在 `blog-web` 运行 `npm run test:weather`，通过模拟上游覆盖权限不被调用、城市/区县、异常降级、缓存、手动选择和请求竞态。
- 后端测试：在 `blog-server` 运行 `mvn test`。

资料：[IPWhois 接口](https://ipwhois.io/documentation)、[IP2Location.io 字段与套餐](https://www.ip2location.io/ip2location-documentation)、[IPAPI.is 匿名接口](https://ipapi.is/developers.html)、[ipapi.co 接口](https://ipapi.co/api/)。
