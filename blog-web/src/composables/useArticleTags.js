import { computed, ref, getCurrentInstance, onScopeDispose } from 'vue'
import { adminCreateTag } from '@/api/admin'
import { ElMessage } from 'element-plus'

/**
 * 包装文章编辑页的标签选择逻辑:
 *  - el-select 启用了 allow-create 后,新输入的标签会以字符串形式进入 model
 *  - 我们把这些字符串就地调 adminCreateTag 创建,再替换成真实数字 ID
 *  - 提供 ensureResolved() 给 save() 前同步等待所有异步创建完成
 *  - 提供 mergeInto(ids) 把任意现有列表里的字符串一并解析为 ID
 */
export function useArticleTags(allTagsRef) {
  // 待处理的字符串新标签 -> Promise<Tag>
  const pending = new Map()
  const creating = ref(false)
  // 组件销毁后,不再把创建结果写回 allTagsRef,避免 Vue setState 警告
  let disposed = false
  try {
    if (getCurrentInstance()) onScopeDispose(() => { disposed = true })
  } catch (_) { /* 非 setup 上下文,忽略 */ }

  const knownByName = computed(() => {
    const m = new Map()
    for (const t of (allTagsRef.value || [])) m.set(t.name, t)
    return m
  })

  // 数字 ID 才保留;字符串就视作新标签
  const splitIds = (ids) => {
    const ok = []
    const newNames = []
    for (const v of (ids || [])) {
      if (typeof v === 'number' || (typeof v === 'string' && /^\d+$/.test(v))) {
        ok.push(Number(v))
      } else if (typeof v === 'string' && v.trim()) {
        newNames.push(v.trim())
      }
    }
    return { ok, newNames }
  }

  // 把单个新名称创建出来,带去重和加载态管理
  const createOne = async (name) => {
    // 1. 已经存在同名标签就直接复用(防止用户连续敲同一个名字)
    const exist = knownByName.value.get(name)
    if (exist) return exist
    // 2. 已经在创建中(同名并发),复用同一 Promise
    if (pending.has(name)) return pending.get(name)
    // 3. 调后端创建
    const p = (async () => {
      try {
        const resp = await adminCreateTag({ name })
        const tag = resp.data || resp
        if (!tag || !tag.id) throw new Error('创建标签返回数据异常')
        // 4. 推入本地列表,后续下拉能立刻看到(组件未销毁才写)
        if (!disposed && !allTagsRef.value.some(t => t.id === tag.id)) {
          allTagsRef.value.push(tag)
        }
        return tag
      } finally {
        pending.delete(name)
      }
    })()
    pending.set(name, p)
    return p
  }

  /**
   * 把列表里的字符串元素就地解析为数字 ID,
   * 同步等待所有异步创建完成。
   * @param {Array} ids 形如 [1, 2, "Java实战"]
   * @returns {Promise<number[]>} 仅数字 ID
   */
  const resolve = async (ids) => {
    const { ok, newNames } = splitIds(ids)
    if (newNames.length === 0) return ok
    creating.value = true
    try {
      const tags = await Promise.all(newNames.map(createOne))
      const newIds = tags.map(t => t.id)
      return [...ok, ...newIds]
    } catch (e) {
      ElMessage.error('新标签创建失败: ' + (e?.message || '未知错误'))
      throw e
    } finally {
      creating.value = false
    }
  }

  return {
    creating,
    resolve,
    // 工具:把 form.tagIds 转成最终要提交的纯数字数组
    resolveAll: (ids) => resolve(ids)
  }
}