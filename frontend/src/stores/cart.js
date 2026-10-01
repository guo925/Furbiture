import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartAPI } from '../api'

export const useCartStore = defineStore('cart', () => {
  const cartItems = ref([])
  const loading = ref(false)

  const totalPrice = computed(() => {
    return cartItems.value.reduce((total, item) => {
      const price = item.product?.price || 0
      const quantity = item.quantity || 0
      return total + price * quantity
    }, 0)
  })

  const totalQuantity = computed(() => {
    return cartItems.value.reduce((total, item) => {
      return total + (item.quantity || 0)
    }, 0)
  })

  // 获取购物车列表
  // 后端 GET /api/carts 已随每条购物车行返回商品快照（CartItemVO.product），
  // 因此这里不再对每条购物车项补查商品详情 —— 一次请求即可拿到全部展示所需数据。
  const getCartList = async () => {
    try {
      loading.value = true
      const response = await cartAPI.getList()
      // 确保正确获取购物车列表数据
      const carts = response.data.data || response.data || []

      // 清除旧数据，确保使用最新的购物车列表
      cartItems.value = []
      // 确保使用普通数组，避免Proxy对象导致的问题
      cartItems.value = [...carts]
    } catch (error) {
      console.error('获取购物车失败:', error?.message)
      cartItems.value = []
    } finally {
      loading.value = false
    }
  }

  // 添加到购物车
  const addToCart = async (productId, quantity = 1) => {
    try {
      const response = await cartAPI.add({
        productId,
        quantity
      })
      await getCartList()
      return response.data
    } catch (error) {
      throw error.response?.data || { code: 500, message: '添加购物车失败' }
    }
  }

  // 更新购物车数量
  const updateCartItem = async (id, quantity) => {
    try {
      const response = await cartAPI.update(id, {
        quantity
      })
      await getCartList()
      return response.data
    } catch (error) {
      console.error('更新购物车失败:', error?.message)
      throw {
        message: error.response?.data?.msg || error.response?.data?.message || '更新失败'
      }
    }
  }

  // 删除购物车商品
  const removeCartItem = async id => {
    try {
      const response = await cartAPI.remove(id)
      await getCartList()
      return response.data
    } catch (error) {
      console.error('删除购物车商品失败:', error?.message)
      throw {
        message: error.response?.data?.msg || error.response?.data?.message || '删除失败'
      }
    }
  }

  // 清空购物车
  const clearCart = async () => {
    try {
      const response = await cartAPI.clear()
      cartItems.value = []
      return response.data
    } catch (error) {
      throw error.response?.data || { code: 500, message: '清空购物车失败' }
    }
  }

  return {
    cartItems,
    loading,
    totalPrice,
    totalQuantity,
    getCartList,
    addToCart,
    updateCartItem,
    removeCartItem,
    clearCart
  }
})
