<template>
  <el-dialog v-model="visible" title="批量导入零件" width="800px" @close="handleClose">
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="粘贴文本" name="text">
        <div class="tab-content">
          <p class="hint">粘贴表格数据，支持 Tab / 逗号 / 空格分隔。第一行应为表头。</p>
          <el-input
            v-model="textInput"
            type="textarea"
            :rows="10"
            placeholder="物料编码	物料名称	入库数量	箱数&#10;ABC001	螺栓	100	2&#10;DEF002	螺母	200	4"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane label="上传Excel" name="excel">
        <div class="tab-content">
          <p class="hint">支持 .xlsx / .xls 文件，第一行为表头。</p>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处 或 <em>点击上传</em></div>
          </el-upload>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 分隔符选择（仅文本模式） -->
    <div class="controls" v-if="activeTab === 'text'">
      <span class="control-label">分隔符：</span>
      <el-radio-group v-model="delimiter" size="small">
        <el-radio-button value="auto">自动检测</el-radio-button>
        <el-radio-button value="tab">Tab</el-radio-button>
        <el-radio-button value="comma">逗号</el-radio-button>
      </el-radio-group>
      <el-button size="small" type="primary" @click="doPreview" style="margin-left:12px">预览</el-button>
    </div>
    <div class="controls" v-if="activeTab === 'excel'">
      <el-button size="small" type="primary" @click="doPreview" :disabled="!uploadedFile">预览</el-button>
    </div>

    <!-- 预览结果 -->
    <div v-if="previewDone" class="preview-section">
      <div class="preview-summary">
        <span>共识别 <b>{{ parsedRows.length }}</b> 行，</span>
        <span class="matched">匹配 <b>{{ matchedCount }}</b> 条</span>
        <span v-if="errorCount > 0" class="unmatched">，失败 <b>{{ errorCount }}</b> 条</span>
      </div>

      <el-table :data="matchResults" border stripe max-height="300" size="small" class="preview-table"
        :row-class-name="previewRowClass">
        <el-table-column prop="partCode" label="物料编码" width="120" />
        <el-table-column prop="partName" label="物料名称" min-width="130" />
        <el-table-column label="入库数量" width="100" align="right">
          <template #default="{ row }">{{ row.plannedQty }}</template>
        </el-table-column>
        <el-table-column label="箱数" width="80" align="right">
          <template #default="{ row }">{{ row.boxCount }}</template>
        </el-table-column>
        <el-table-column label="匹配状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.matched" :type="row.matchType === 'exact-code' ? 'success' : 'warning'" size="small">
              {{ row.matchType === 'exact-code' ? '已匹配' : '名称匹配' }}
            </el-tag>
            <el-tag v-else type="danger" size="small">{{ row.error || '未找到' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 错误摘要 -->
      <el-collapse v-if="errorCount > 0" style="margin-top:8px">
        <el-collapse-item :title="`错误详情 (${errorCount} 条)`">
          <el-table :data="errorRows" border size="small" max-height="200">
            <el-table-column prop="rowIndex" label="行号" width="60" />
            <el-table-column prop="rawData" label="原始数据" min-width="200" />
            <el-table-column prop="error" label="错误原因" min-width="180" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 提示：未选择供应商 -->
    <el-alert v-if="!supplierId" type="warning" title="请先选择供应商" :closable="false" show-icon
      style="margin-top:12px" />

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="doImport" :disabled="!canImport">
        导入 ({{ matchedCount }} 条)
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { Part } from '@/types/inbound'

// ============ 接口 ============

interface ParsedRow {
  partCode: string
  partName: string
  plannedQty: number
  boxCount: number
  rawData: string
  rowIndex: number
}

interface MatchResult {
  parsed: ParsedRow
  matched: boolean
  matchType: 'exact-code' | 'name-only' | 'none'
  partId?: number
  part?: Part
  partCode: string
  partName: string
  plannedQty: number
  boxCount: number
  /** 包装容量 */
  capacity: number
  /** 库区ID */
  warehouseAreaId?: number
  error?: string
}

// ============ Props / Emits ============

const props = defineProps<{
  supplierId?: number
  allParts: Part[]
}>()

const emit = defineEmits<{
  import: [parts: ImportedPart[]]
  close: []
}>()

/** 导出给 PartsTable 使用的结构 */
export interface ImportedPart {
  partId: number
  partCode: string
  partName: string
  plannedQty: number
  boxCount: number
  /** 包装容量 */
  capacity: number
  warehouseAreaId?: number
  batchNo?: string
}

// ============ 状态 ============

const visible = ref(true)
const activeTab = ref('text')
const textInput = ref('')
const delimiter = ref<'auto' | 'tab' | 'comma'>('auto')
const uploadedFile = ref<File | null>(null)
const uploadRef = ref()

// 解析结果
const parsedRows = ref<ParsedRow[]>([])
const matchResults = ref<MatchResult[]>([])
const previewDone = ref(false)

const matchedCount = computed(() => matchResults.value.filter(r => r.matched).length)
const errorCount = computed(() => matchResults.value.filter(r => !r.matched).length)
const errorRows = computed(() =>
  matchResults.value
    .filter(r => !r.matched)
    .map(r => ({ rowIndex: r.parsed.rowIndex + 1, rawData: r.parsed.rawData, error: r.error || '未知错误' }))
)
const canImport = computed(() => matchedCount.value > 0)

// ============ 表头识别 ============

const CODE_ALIASES = ['物料编码', '零件编码', '编码', 'partcode', 'code', '物料编号']
const NAME_ALIASES = ['物料名称', '零件名称', '名称', 'partname', 'name', '品名']
const QTY_ALIASES = ['入库数量', '计划数量', '数量', 'plannedqty', 'quantity', 'qty', '件数']
const BOX_ALIASES = ['箱数', '箱', 'boxcount', 'box_count', 'boxcount']

function normalizeHeader(h: string): string {
  return h.trim().replace(/[\s_-]/g, '').toLowerCase()
}

interface ColumnMap {
  codeIdx: number
  nameIdx: number
  qtyIdx: number
  boxIdx: number
}

function mapColumns(headers: string[]): ColumnMap | null {
  const map: ColumnMap = { codeIdx: -1, nameIdx: -1, qtyIdx: -1, boxIdx: -1 }
  headers.forEach((h, i) => {
    const n = normalizeHeader(h)
    if (CODE_ALIASES.some(a => normalizeHeader(a) === n)) map.codeIdx = i
    if (NAME_ALIASES.some(a => normalizeHeader(a) === n)) map.nameIdx = i
    if (QTY_ALIASES.some(a => normalizeHeader(a) === n)) map.qtyIdx = i
    if (BOX_ALIASES.some(a => normalizeHeader(a) === n)) map.boxIdx = i
  })
  // 至少需要物料编码或物料名称
  if (map.codeIdx < 0 && map.nameIdx < 0) return null
  return map
}

// ============ 解析逻辑 ============

function detectDelimiter(line: string): string {
  const tabs = (line.match(/\t/g) || []).length
  const commas = (line.match(/,/g) || []).length
  if (tabs >= commas && tabs > 0) return '\t'
  if (commas > 0) return ','
  return '\t'
}

function parseText(): ParsedRow[] {
  const text = textInput.value.trim()
  if (!text) return []

  const lines = text.split(/\r?\n/).filter(l => l.trim())
  if (lines.length < 2) return [] // 需要表头 + 至少一行数据

  let sep: string
  if (delimiter.value === 'auto') {
    sep = detectDelimiter(lines[0])
  } else if (delimiter.value === 'tab') {
    sep = '\t'
  } else {
    sep = ','
  }

  const headers = lines[0].split(sep).map(h => h.trim()).filter(h => h)
  const dataRows = lines.slice(1).map(l => l.split(sep).map(c => c.trim()))
  return parseRows(headers, dataRows, sep)
}

async function parseExcel(): Promise<ParsedRow[]> {
  if (!uploadedFile.value) return []
  const XLSX = await import('xlsx')

  const data = await new Promise<ArrayBuffer>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target!.result as ArrayBuffer)
    reader.onerror = reject
    reader.readAsArrayBuffer(uploadedFile.value!)
  })

  const workbook = XLSX.read(data, { type: 'array', cellDates: true })
  const sheetName = workbook.SheetNames[0]
  if (!sheetName) return []

  const sheet = workbook.Sheets[sheetName]
  const jsonData = XLSX.utils.sheet_to_json<string[]>(sheet, { header: 1, defval: '' })

  if (jsonData.length < 2) return []

  const headers = (jsonData[0] as string[]).map(h => String(h).trim()).filter(h => h)
  const rows = jsonData.slice(1).map(r =>
    (r as string[]).map(c => String(c).trim())
  )
  return parseRows(headers, rows, null)
}

function parseRows(headers: string[], rows: string[][], sep: string | null): ParsedRow[] {
  const colMap = mapColumns(headers)
  if (!colMap) return []

  const results: ParsedRow[] = []

  rows.forEach((cols, idx) => {
    // 跳过全空行
    if (cols.every(c => !c)) return

    const partCode = colMap.codeIdx >= 0 ? (cols[colMap.codeIdx] || '').trim() : ''
    const partName = colMap.nameIdx >= 0 ? (cols[colMap.nameIdx] || '').trim() : ''
    const qtyStr = colMap.qtyIdx >= 0 ? (cols[colMap.qtyIdx] || '').trim() : ''
    const boxStr = colMap.boxIdx >= 0 ? (cols[colMap.boxIdx] || '').trim() : ''

    // 如果编码和名称都为空则跳过
    if (!partCode && !partName) return

    const plannedQty = qtyStr ? parseFloat(qtyStr) : NaN
    const boxCount = boxStr ? parseFloat(boxStr) : NaN

    results.push({
      partCode,
      partName,
      plannedQty: isNaN(plannedQty) ? 0 : plannedQty,
      boxCount: isNaN(boxCount) ? 0 : boxCount,
      rawData: sep ? cols.join(sep) : cols.join('\t'),
      rowIndex: idx,
    })
  })

  return results
}

// ============ 匹配逻辑 ============

function matchParts(rows: ParsedRow[]): MatchResult[] {
  const parts = props.allParts

  return rows.map(row => {
    let match: Part | undefined
    let matchType: MatchResult['matchType'] = 'none'
    let error: string | undefined

    const code = row.partCode.trim()
    const name = row.partName.trim()

    // Step 1: 按编码精确匹配
    if (code) {
      const byCode = parts.filter(p => p.code && p.code.trim().toLowerCase() === code.toLowerCase())
      if (byCode.length === 1) {
        match = byCode[0]
        matchType = 'exact-code'
      } else if (byCode.length > 1) {
        match = byCode[0]
        matchType = 'exact-code'
      }
    }

    // Step 2: 编码未匹配，按名称匹配
    if (!match && name) {
      const byName = parts.filter(p => p.name && p.name.trim().toLowerCase() === name.toLowerCase())
      if (byName.length === 1) {
        match = byName[0]
        matchType = 'name-only'
      } else if (byName.length > 1) {
        match = byName[0]
        matchType = 'name-only'
      }
    }

    // Step 3: 未匹配
    if (!match) {
      if (code && !name) error = '物料编码未找到'
      else if (!code && name) error = '物料名称未找到'
      else error = '物料编码/名称未在供应商零件列表中找到'
    }

    // 数量计算
    const capacity = match?.packageCapacity || 1
    let plannedQty = row.plannedQty
    let boxCount = row.boxCount

    if (plannedQty > 0 && boxCount > 0) {
      // 都有：数量优先
      boxCount = round(plannedQty / capacity)
    } else if (plannedQty > 0) {
      boxCount = round(plannedQty / capacity)
    } else if (boxCount > 0) {
      plannedQty = round(boxCount * capacity)
    } else {
      // 都没有：默认1箱
      boxCount = 1
      plannedQty = round(capacity)
    }

    // 负数校验
    if (plannedQty < 0) {
      plannedQty = 0
      boxCount = 0
      if (!error) error = '数量不能为负数'
    }
    if (boxCount < 0) {
      boxCount = 0
      if (!error) error = '箱数不能为负数'
    }

    return {
      parsed: row,
      matched: !!match,
      matchType: match ? matchType : 'none',
      partId: match?.id,
      part: match,
      partCode: row.partCode || match?.code || '',
      partName: row.partName || match?.name || '',
      plannedQty,
      boxCount,
      capacity,
      warehouseAreaId: match?.warehouseAreaId,
      error,
    }
  })
}

function round(value: number, precision = 2): number {
  const factor = 10 ** precision
  return Math.round((value + Number.EPSILON) * factor) / factor
}

// ============ 预览 ============

async function doPreview() {
  let rows: ParsedRow[] = []

  if (activeTab.value === 'text') {
    rows = parseText()
    if (rows.length === 0) {
      ElMessage.warning('未能解析到有效数据，请检查格式')
      previewDone.value = false
      return
    }
  } else {
    rows = await parseExcel()
    if (rows.length === 0) {
      ElMessage.warning('未能解析到有效数据，请检查文件格式')
      previewDone.value = false
      return
    }
  }

  parsedRows.value = rows
  matchResults.value = matchParts(rows)
  previewDone.value = true
}

// ============ 文件上传 ============

function onFileChange(file: any) {
  uploadedFile.value = file.raw
  previewDone.value = false
}

function onFileRemove() {
  uploadedFile.value = null
  previewDone.value = false
}

// ============ 导入 ============

function doImport() {
  const matched = matchResults.value.filter(r => r.matched)
  if (matched.length === 0) {
    ElMessage.warning('没有可导入的零件')
    return
  }

  const parts: ImportedPart[] = matched.map(r => ({
    partId: r.partId!,
    partCode: r.partCode,
    partName: r.partName,
    plannedQty: r.plannedQty,
    boxCount: r.boxCount,
    capacity: r.capacity,
    warehouseAreaId: r.warehouseAreaId,
    batchNo: '',
  }))

  emit('import', parts)
  visible.value = false
}

// ============ 行样式 ============

function previewRowClass({ row }: { row: MatchResult }) {
  if (!row.matched) return 'row-error'
  if (row.matchType === 'name-only') return 'row-warning'
  return ''
}

// ============ 关闭 ============

function handleClose() {
  textInput.value = ''
  uploadedFile.value = null
  parsedRows.value = []
  matchResults.value = []
  previewDone.value = false
  activeTab.value = 'text'
  emit('close')
}

// 供应商变化时关闭弹窗
watch(() => props.supplierId, () => {
  visible.value = false
})
</script>

<style scoped>
.tab-content {
  margin-bottom: 8px;
}

.hint {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px;
}

.controls {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.control-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}

.preview-section {
  margin-top: 12px;
}

.preview-summary {
  font-size: 13px;
  margin-bottom: 8px;
  color: #606266;
}

.preview-summary .matched {
  color: #67c23a;
}

.preview-summary .unmatched {
  color: #f56c6c;
}

.preview-table {
  margin-top: 4px;
}

:deep(.row-error) {
  background-color: #fef0f0 !important;
}

:deep(.row-warning) {
  background-color: #fdf6ec !important;
}
</style>
