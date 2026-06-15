import type { InjectionKey } from 'vue'

/** 全局 Toast 消息提示 */
export const SHOW_TOAST: InjectionKey<(msg: string, duration?: number) => void> = Symbol('showToast')

/** 打开登录弹窗 */
export const SHOW_AUTH: InjectionKey<() => void> = Symbol('showAuth')
