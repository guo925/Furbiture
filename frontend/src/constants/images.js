/**
 * 图片占位常量
 *
 * 全项目此前把同一个 Unsplash 外链硬编码在 9 个文件里，改一次要改 9 处。
 * 这里按用途分两个尺寸（列表缩略图 / 详情大图），避免"顺手统一尺寸"导致的显示变化。
 */

/** 列表缩略图占位（w=300） */
export const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=300&q=80'

/** 详情 / 卡片大图占位（w=900） */
export const FALLBACK_IMAGE_LARGE =
  'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80'
