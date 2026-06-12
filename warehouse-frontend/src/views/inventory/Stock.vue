<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="搜索">
          <el-input
            v-model="query.keyword"
            placeholder="零件编码或名称"
            clearable
            style="width: 220px"
            @keyup.enter="fetchData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :default-sort="{ prop: 'totalStock', order: 'descending' }"
        @expand-change="loadKanbans"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div v-if="kanbanMap[row.partId]?.length" style="padding:8px 20px">
              <h4 style="margin:0 0 8px">库存看板明细</h4>
              <el-table :data="kanbanMap[row.partId]" size="small" border>
                <el-table-column prop="kanbanNo" label="看板号" min-width="240" />
                <el-table-column prop="boxSeq" label="箱号" width="60" align="center">
                  <template #default="{ row: k }">C-{{ k.boxSeq }}</template>
                </el-table-column>
                <el-table-column prop="quantity" label="箱数量" width="80" align="center" />
                <el-table-column prop="warehouseAreaName" label="库区" width="120" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: k }">
                    <el-tag :type="['info','success','danger'][k.status]" size="small">
                      {{ ['待入库','已入库','已出库'][k.status] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="inboundOrderNo" label="入库单号" width="140" />
                <el-table-column prop="createTime" label="入库时间" width="170" />
              </el-table>
            </div>
            <div v-else style="padding:8px 20px;color:#909399">暂无看板记录</div>
          </template>
        </el-table-column>
        <el-table-column prop="partCode" label="零件编码" width="120" sortable="custom" />
        <el-table-column prop="partName" label="零件名称" min-width="140" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="packageCapacity" label="包装容量" width="90" align="center" />
        <el-table-column prop="totalStock" label="库存总量" width="110" align="center" sortable="custom">
          <template #default="{ row }">
            <span class="stock-qty">{{ row.totalStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库区分布" min-width="200">
          <template #default="{ row }">
            <div class="area-tags">
              <el-tag v-for="a in row.areaStocks" :key="a.areaId" size="small" effect="plain" style="margin:2px 4px 2px 0">
                {{ a.areaName }}: {{ a.quantity }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="summary" v-if="tableData.length > 0">
        共 {{ tableData.length }} 种零件，库存总量 {{ totalAll }}
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getStockListApi, type InventoryVO } from '@/api/inventory'
import { listKanbansByPart, type Kanban } from '@/api/kanban'

const loading = ref(false)
const tableData = ref<InventoryVO[]>([])
const kanbanMap = ref<Record<number, Kanban[]>>({})

const query = reactive({
  keyword: '',
})

const totalAll = computed(() =>
  tableData.value.reduce((s, r) => s + (r.totalStock || 0), 0)
)

async function fetchData() {
  loading.value = true
  try {
    const res = await getStockListApi(query.keyword || undefined)
    tableData.value = res.data || []
  } catch { /* ignore */ } finally { loading.value = false }
}

async function loadKanbans(row: any, expandedRows: any[]) {
  if (expandedRows.some((r: any) => r.partId === row.partId)) {
    if (!kanbanMap.value[row.partId]) {
      try {
        const res = await listKanbansByPart(row.partId)
        kanbanMap.value[row.partId] = res.data || []
      } catch { /* */ }
    }
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.stock-qty {
  font-weight: bold;
  color: #409eff;
  font-size: 15px;
}
.summary {
  margin-top: 12px;
  text-align: right;
  color: #909399;
  font-size: 13px;
}
</style>
