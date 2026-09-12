<template>
  <UserLayout>
    <section class="shop-container address-page">
      <div class="page-head">
        <div>
          <h1>地址管理</h1>
          <p>管理收货地址、默认地址和配送联系信息</p>
        </div>
        <el-button type="primary" @click="handleAdd">新增地址</el-button>
      </div>

      <div class="address-tips">
        <span>已保存 {{ addresses.length }} 个地址</span>
        <span>默认地址会在结算时优先使用</span>
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />

      <div v-else-if="addresses.length === 0" class="empty">
        <el-empty description="暂无收货地址" />
        <el-button type="primary" @click="handleAdd">添加收货地址</el-button>
      </div>

      <div v-else class="address-grid">
        <article
          v-for="address in addresses"
          :key="address.id"
          :class="['address-card', { default: address.isDefault }]"
        >
          <header>
            <div>
              <strong>{{ address.receiver }}</strong>
              <span>{{ maskPhone(address.phone) }}</span>
            </div>
            <el-tag v-if="address.isDefault" type="danger" effect="light">默认地址</el-tag>
          </header>

          <p>{{ fullAddress(address) }}</p>

          <footer>
            <button type="button" @click="copyAddress(address)">复制地址</button>
            <button v-if="!address.isDefault" type="button" @click="setDefault(address.id)">设为默认</button>
            <button type="button" @click="handleEdit(address)">修改</button>
            <button type="button" class="danger" @click="handleDelete(address.id)">删除</button>
          </footer>
        </article>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <div class="smart-paste">
        <el-input
          v-model="smartText"
          type="textarea"
          :rows="2"
          placeholder="粘贴姓名 手机号 省市区详细地址，可自动拆分"
        />
        <el-button @click="parseSmartText">智能识别</el-button>
      </div>

      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="96px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addressForm.receiver" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" required>
          <div class="region-row">
            <el-form-item prop="province">
              <el-input v-model="addressForm.province" placeholder="省/自治区" />
            </el-form-item>
            <el-form-item prop="city">
              <el-input v-model="addressForm.city" placeholder="市" />
            </el-form-item>
            <el-form-item prop="district">
              <el-input v-model="addressForm.district" placeholder="区/县" />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="addressForm.detail"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            placeholder="街道、小区、楼栋门牌号"
          />
        </el-form-item>
        <el-form-item label="地址标签">
          <el-radio-group v-model="addressForm.tag">
            <el-radio-button label="家" />
            <el-radio-button label="公司" />
            <el-radio-button label="学校" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" active-text="设为默认" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存地址</el-button>
      </template>
    </el-dialog>
  </UserLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserLayout from '../../components/UserLayout.vue'
import { addressAPI } from '../../api'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const loading = ref(false)
const saving = ref(false)
const addresses = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增收货地址')
const addressFormRef = ref(null)
const smartText = ref('')

const addressForm = reactive({
  id: null,
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  tag: '家',
  isDefault: false
})

const validatePhone = (rule, value, callback) => {
  if (!/^1[3-9]\d{9}$/.test(value || '')) {
    callback(new Error('请输入11位中国大陆手机号'))
    return
  }
  callback()
}

const addressRules = {
  receiver: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login')
    return
  }
  await loadAddresses()
  await cartStore.getCartList()
})

const loadAddresses = async () => {
  try {
    loading.value = true
    const response = await addressAPI.getList()
    addresses.value = (response.data.data || []).map(normalizeAddress)
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '获取地址失败')
  } finally {
    loading.value = false
  }
}

const normalizeAddress = addr => ({
  id: addr.id,
  receiver: addr.name,
  phone: addr.phone,
  province: addr.province,
  city: addr.city,
  district: addr.district,
  detail: addr.detailAddress,
  tag: addr.tag || inferTag(addr.detailAddress),
  isDefault: addr.isDefault === 1
})

const handleAdd = () => {
  dialogTitle.value = '新增收货地址'
  smartText.value = ''
  Object.assign(addressForm, {
    id: null,
    receiver: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    tag: '家',
    isDefault: addresses.value.length === 0
  })
  dialogVisible.value = true
}

const handleEdit = address => {
  dialogTitle.value = '修改收货地址'
  smartText.value = ''
  Object.assign(addressForm, address)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!addressFormRef.value) return
  await addressFormRef.value.validate()

  const payload = {
    name: addressForm.receiver.trim(),
    phone: addressForm.phone.trim(),
    province: addressForm.province.trim(),
    city: addressForm.city.trim(),
    district: addressForm.district.trim(),
    detailAddress: addressForm.detail.trim(),
    isDefault: addressForm.isDefault ? 1 : 0
  }

  try {
    saving.value = true
    if (addressForm.id) {
      await addressAPI.update(addressForm.id, payload)
      ElMessage.success('地址已更新')
    } else {
      await addressAPI.create(payload)
      ElMessage.success('地址已添加')
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const setDefault = async id => {
  try {
    await addressAPI.setDefault(id)
    ElMessage.success('默认地址已更新')
    await loadAddresses()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '设置失败')
  }
}

const handleDelete = async id => {
  try {
    await ElMessageBox.confirm('删除后该地址不可恢复，确定删除吗？', '删除地址', { type: 'warning' })
    await addressAPI.remove(id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.msg || '删除失败')
  }
}

const parseSmartText = () => {
  const text = smartText.value.trim().replace(/\s+/g, ' ')
  const phone = text.match(/1[3-9]\d{9}/)?.[0]
  if (phone) addressForm.phone = phone

  const withoutPhone = phone ? text.replace(phone, '').trim() : text
  const parts = withoutPhone.split(' ').filter(Boolean)
  if (parts.length > 0) addressForm.receiver = parts[0]

  const addressText = parts.slice(1).join('')
  const region = addressText.match(/^(.+?(省|自治区|市))(.+?市)?(.+?(区|县|旗))?(.*)$/)
  if (region) {
    addressForm.province = region[1] || addressForm.province
    addressForm.city = (region[3] || '').replace(/市$/, '市') || addressForm.city
    addressForm.district = region[4] || addressForm.district
    addressForm.detail = region[6] || addressForm.detail
  } else if (addressText) {
    addressForm.detail = addressText
  }
}

const copyAddress = async address => {
  await copyToClipboard(`${address.receiver} ${address.phone} ${fullAddress(address)}`)
  ElMessage.success('地址已复制')
}

const fullAddress = address => `${address.province} ${address.city} ${address.district} ${address.detail}`.replace(/\s+/g, ' ').trim()
const maskPhone = phone => phone ? `${phone.slice(0, 3)}****${phone.slice(7)}` : ''
const inferTag = detail => {
  if (/公司|园区|写字楼|大厦/.test(detail || '')) return '公司'
  if (/学校|大学|学院|校区/.test(detail || '')) return '学校'
  return '家'
}
const copyToClipboard = async text => {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const input = document.createElement('textarea')
  input.value = text
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  document.body.removeChild(input)
}
</script>

<style scoped>
.address-page {
  padding: 28px 20px 48px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.page-head p,
.address-tips,
.address-card p {
  color: #606266;
}

.address-tips {
  display: flex;
  gap: 20px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
}

.empty {
  padding: 60px 0;
  background: #fff;
  border-radius: 8px;
  text-align: center;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 14px;
}

.address-card {
  min-height: 176px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.address-card.default {
  border-color: #f56c6c;
}

.address-card header,
.address-card header div,
.address-card footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.address-card header {
  justify-content: space-between;
}

.address-card strong {
  font-size: 18px;
}

.address-card p {
  line-height: 1.7;
  margin: 18px 0;
}

.address-card footer {
  flex-wrap: wrap;
}

.address-card button {
  border: 0;
  background: none;
  color: var(--color-primary);
  cursor: pointer;
}

.address-card button.danger {
  color: #f56c6c;
}

.smart-paste {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin-bottom: 18px;
}

.region-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  width: 100%;
}

.region-row :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 720px) {
  .page-head,
  .smart-paste {
    grid-template-columns: 1fr;
    align-items: flex-start;
    display: grid;
  }

  .address-tips,
  .region-row {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
