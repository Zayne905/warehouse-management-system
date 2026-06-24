<template>
  <el-card shadow="never" class="chat-card" :body-style="{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }">
    <template #header>
      <div class="chat-header">
        <span>🤖 AI 仓库助手</span>
        <el-button text size="small" @click="clearChat" :disabled="messages.length === 0">
          清空对话
        </el-button>
      </div>
    </template>

    <!-- Messages -->
    <div class="chat-messages" ref="msgContainer">
      <div v-if="messages.length === 0" class="chat-welcome">
        <p>👋 你好！我是仓库AI助手，可以帮你：</p>
        <ul>
          <li>查询库存状况</li>
          <li>查看订单进度</li>
          <li>获取今日运营摘要</li>
          <li>搜索零件和供应商</li>
          <li>查看出入库趋势</li>
        </ul>
        <div class="quick-actions">
          <el-button
            v-for="qa in quickActions"
            :key="qa"
            size="small"
            @click="sendMessage(qa)"
          >
            {{ qa }}
          </el-button>
        </div>
      </div>
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="chat-msg"
        :class="msg.role"
      >
        <div class="msg-avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="msg-content">
          <div class="msg-text" v-html="renderMarkdown(msg.content)" />
          <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>
      <div v-if="streaming" class="chat-msg assistant">
        <div class="msg-avatar">🤖</div>
        <div class="msg-content">
          <div class="msg-text streaming" v-html="renderMarkdown(streamBuffer) || '<span class=cursor>▊</span>'" />
        </div>
      </div>
    </div>

    <!-- Input -->
    <div class="chat-input">
      <el-input
        v-model="input"
        placeholder="输入你的问题，如：今天入库了多少？"
        :disabled="streaming"
        @keydown.enter.exact="sendMessage(input)"
        clearable
      >
        <template #append>
          <el-button
            :icon="streaming ? 'Close' : 'Promotion'"
            :type="streaming ? 'danger' : 'primary'"
            @click="streaming ? stopStreaming() : sendMessage(input)"
          />
        </template>
      </el-input>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { streamChat, clearSession, type ChatMessage } from '@/api/ai'

const SESSION_ID = 'dashboard-' + Math.random().toString(36).substring(7)

const md = new MarkdownIt({ breaks: true })

const messages = ref<ChatMessage[]>([])
const input = ref('')
const streaming = ref(false)
const streamBuffer = ref('')
const msgContainer = ref<HTMLElement>()
let abortController: AbortController | null = null

const quickActions = [
  '今日运营摘要',
  '当前库存概况',
  '最近7天出入库趋势',
  '有哪些待处理的订单？',
]

function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    return md.render(text)
  } catch {
    return text.replace(/\n/g, '<br>')
  }
}

function formatTime(ts?: number): string {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

function sendMessage(text: string) {
  const msg = text.trim()
  if (!msg || streaming.value) return

  input.value = ''

  // Add user message
  messages.value.push({
    role: 'user',
    content: msg,
    timestamp: Date.now(),
  })
  scrollToBottom()

  // Start streaming
  streaming.value = true
  streamBuffer.value = ''

  abortController = streamChat(
    msg,
    SESSION_ID,
    (chunk) => {
      streamBuffer.value += chunk
      scrollToBottom()
    },
    () => {
      // Done
      if (streamBuffer.value) {
        messages.value.push({
          role: 'assistant',
          content: streamBuffer.value,
          timestamp: Date.now(),
        })
      }
      streamBuffer.value = ''
      streaming.value = false
      scrollToBottom()
    },
    (err) => {
      ElMessage.error(err)
      streaming.value = false
      streamBuffer.value = ''
    }
  )
}

function stopStreaming() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  if (streamBuffer.value) {
    messages.value.push({
      role: 'assistant',
      content: streamBuffer.value,
      timestamp: Date.now(),
    })
  }
  streamBuffer.value = ''
  streaming.value = false
}

async function clearChat() {
  stopStreaming()
  messages.value = []
  await clearSession(SESSION_ID)
  ElMessage.success('对话已清空')
}

onBeforeUnmount(() => {
  stopStreaming()
})
</script>

<style scoped>
.chat-card {
  height: calc(100vh - 200px);
  min-height: 600px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-welcome {
  text-align: center;
  padding: 24px 16px;
}

.chat-welcome p {
  font-size: 15px;
  color: #303133;
  margin-bottom: 12px;
}

.chat-welcome ul {
  text-align: left;
  color: #606266;
  font-size: 13px;
  line-height: 2;
  padding-left: 24px;
  margin-bottom: 16px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chat-msg {
  display: flex;
  gap: 10px;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.chat-msg.assistant .msg-avatar {
  background-color: #e6f7ff;
}

.chat-msg.user .msg-avatar {
  background-color: #f0f5ff;
}

.msg-content {
  max-width: 85%;
}

.chat-msg.user .msg-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-msg.user .msg-text {
  background-color: #409EFF;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.assistant .msg-text {
  background-color: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.msg-text.streaming {
  min-height: 20px;
}

.msg-text :deep(p) {
  margin: 4px 0;
}

.msg-text :deep(ul), .msg-text :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

.msg-text :deep(code) {
  background-color: rgba(0,0,0,0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.msg-text :deep(pre) {
  background-color: rgba(0,0,0,0.06);
  padding: 8px 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
}

.msg-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  font-size: 13px;
}

.msg-text :deep(th), .msg-text :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 4px 8px;
  text-align: left;
}

.msg-text :deep(th) {
  background-color: #f5f7fa;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}
</style>
