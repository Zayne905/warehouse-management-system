<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑入库单' : '新增入库单' }}</span>
          <span v-if="form.orderNo" class="order-no">
            单号: {{ form.orderNo }}
            <el-button size="small" text @click="copyOrderNo">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </span>
        </div>
      </template>

      <!-- 基本信息 -->
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="入库单号">
              <el-input
                :model-value="form.orderNo || '保存后自动生成'"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" required>
              <el-select
                v-model="form.supplierId"
                placeholder="请选择供应商"
                style="width: 100%"
                @change="onSupplierChange"
              >
                <el-option
                  v-for="s in supplierList"
                  :key="s.id"
                  :label="s.name"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="订单号">
              <el-input
                v-model="form.orderNumber"
                placeholder="输入采购订单号"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="备注信息"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 零件明细 -->
    <el-card shadow="never" style="margin-top: 12px">
      <template #header>
        <span>零件明细</span>
      </template>
      <PartsTable
        ref="partsTableRef"
        :supplier-id="form.supplierId"
        :order-id="form.id"
        :initial-details="initialDetails"
      />
    </el-card>

    <!-- 底部按钮 -->
    <div class="footer-bar">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSave">保存草稿</el-button>
      <el-button type="success" @click="handleSubmit">提交入库</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { saveInboundApi, submitInboundApi, getInboundDetailApi } from '@/api/inbound'
import { getSupplierListApi } from '@/api/supplier'
import PartsTable from './components/PartsTable.vue'
import type { Supplier, InboundDetailDTO } from '@/types/inbound'

const router = useRouter()
const route = useRoute()

const isEdit = ref(false)
const supplierList = ref<Supplier[]>([])
const partsTableRef = ref<InstanceType<typeof PartsTable>>()
const initialDetails = ref<InboundDetailDTO[]>([])

const form = reactive({
  id: undefined as number | undefined,
  orderNo: '',
  supplierId: undefined as number | undefined,
  orderNumber: '',
  remark: '',
})

onMounted(async () => {
  // 加载供应商列表
  try {
    const res = await getSupplierListApi()
    supplierList.value = res.data
  } catch { /* ignore */ }

  // 编辑模式
  const editId = route.params.id
  if (editId) {
    isEdit.value = true
    try {
      const res = await getInboundDetailApi(Number(editId))
      const order = res.data
      form.id = order.id
      form.orderNo = order.orderNo
      form.supplierId = order.supplierId
      form.orderNumber = order.orderNumber
      form.remark = order.remark || ''

      // 加载已有明细
      if (order.details) {
        initialDetails.value = order.details.map(d => ({
          partId: d.partId,
          plannedQty: d.plannedQty,
          unit: d.unit,
          warehouseAreaId: d.warehouseAreaId,
          batchNo: d.batchNo,
          lineNo: d.lineNo,
        }))
      }
    } catch { /* ignore */ }
  }
})

function onSupplierChange() {
  // PartsTable 会通过 props 响应式更新
}

function copyOrderNo() {
  if (form.orderNo) {
    navigator.clipboard.writeText(form.orderNo)
    ElMessage.success('已复制单号')
  }
}

async function handleSave() {
  await doSave(false)
}

async function handleSubmit() {
  await doSave(true)
}

async function doSave(submit: boolean) {
  if (!form.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }

  const details = partsTableRef.value?.getDetails() || []
  if (details.length === 0) {
    ElMessage.warning('请添加零件明细')
    return
  }

  try {
    const res = await saveInboundApi({
      id: form.id,
      supplierId: form.supplierId,
      orderNumber: form.orderNumber,
      remark: form.remark,
      details,
    })

    if (submit && res.data?.id) {
      await submitInboundApi(res.data.id)
      ElMessage.success('提交成功')
    } else {
      ElMessage.success('保存成功')
    }

    router.push('/inventory/inbound')
  } catch {
    // 错误由拦截器处理
  }
}

function handleCancel() {
  router.back()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-no {
  font-size: 14px;
  color: #409eff;
  font-weight: bold;
}
.footer-bar {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
