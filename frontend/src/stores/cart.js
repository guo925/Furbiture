import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cartAPI, productAPI } from '../api'

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
  const getCartList = async () => {
    try {
      loading.value = true
      const response = await cartAPI.getList()
      console.log('响应数据:', response)
      console.log('响应数据结构:', response.data)
      // 确保正确获取购物车列表数据
      const carts = response.data.data || response.data || []
      console.log('购物车列表数据:', carts)
      
      // 为每个购物车项补充商品信息
      const cartItemsWithProduct = await Promise.all(
        carts.map(async (cart) => {
          console.log('购物车项:', cart)
          try {
            const productResponse = await productAPI.getDetail(cart.productId)
            const productData = productResponse.data.data?.product || productResponse.data?.product || null
            return {
              ...cart,
              product: productData
            }
          } catch (error) {
            console.error(`获取商品 ${cart.productId} 信息失败:`, error)
            return {
              ...cart,
              product: null
            }
          }
        })
      )
      
      // 清除旧数据，确保使用最新的购物车列表
      cartItems.value = []
      // 确保使用普通数组，避免Proxy对象导致的问题
      cartItems.value = [...cartItemsWithProduct]
      console.log('处理后的购物车列表:', cartItems.value)
      // 输出每个购物车项的ID，确保ID正确
      cartItems.value.forEach((item, index) => {
        console.log(`购物车项 ${index} ID:`, item.id)
      })
    } catch (error) {
      console.error('获取购物车失败:', error)
      cartItems.value = []
    } finally {
      loading.value = false
    }
  }

  // 添加到购物车
  const addToCart = async (productId, quantity = 1, showMessage = true) => {
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
      console.log('更新购物车数量 - 购物车ID:', id, '数量:', quantity)
      const response = await cartAPI.update(id, {
        quantity
      })
      console.log('更新购物车成功:', response)
      await getCartList()
      return response.data
    } catch (error) {
      console.error('更新购物车失败:', error)
      console.error('错误响应:', error.response)
      console.error('错误响应数据:', error.response?.data)
      throw {
        message: error.response?.data?.msg || error.response?.data?.message || '更新失败'
      }
    }
  }

  // 删除购物车商品
  const removeCartItem = async (id) => {
    try {
      console.log('删除购物车商品 - 购物车ID:', id)
      const response = await cartAPI.remove(id)
      console.log('删除购物车成功:', response)
      await getCartList()
      return response.data
    } catch (error) {
      console.error('删除购物车商品失败:', error)
      console.error('错误响应:', error.response)
      console.error('错误响应数据:', error.response?.data)
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