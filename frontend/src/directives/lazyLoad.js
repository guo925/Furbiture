/**
 * 图片懒加载指令
 * 使用 IntersectionObserver API，当图片进入视口时才加载
 * 使用方式：<img v-lazy-load="imageUrl" />
 */
const imageCache = new Set()

export default {
  mounted(el, binding) {
    el.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="200"%3E%3Crect fill="%23f5f5f5" width="200" height="200"/%3E%3C/svg%3E'

    const imgUrl = binding.value
    if (!imgUrl) return

    if (imageCache.has(imgUrl)) {
      el.src = imgUrl
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = new Image()
            img.onload = () => {
              el.src = imgUrl
              imageCache.add(imgUrl)
            }
            img.onerror = () => {
              el.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="200"%3E%3Crect fill="%23f5f5f5" width="200" height="200"/%3E%3Ctext x="50%25" y="50%25" text-anchor="middle" fill="%23999"%3E加载失败%3C/text%3E%3C/svg%3E'
            }
            img.src = imgUrl
            observer.unobserve(el)
          }
        })
      },
      { rootMargin: '100px' }
    )
    observer.observe(el)
    el._lazyObserver = observer
  },
  unmounted(el) {
    if (el._lazyObserver) {
      el._lazyObserver.disconnect()
    }
  }
}
