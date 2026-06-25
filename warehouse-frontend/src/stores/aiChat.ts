import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
}

export interface ChatConversation {
  id: string
  title: string
  messages: ChatMessage[]
  createdAt: number
}

const STORAGE_KEY = 'ai_chat_store'

function loadFromStorage(): { conversations: ChatConversation[]; activeId: string | null } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const data = JSON.parse(raw)
      if (data.conversations && Array.isArray(data.conversations)) {
        return data
      }
    }
  } catch { /* ignore */ }
  return { conversations: [], activeId: null }
}

function saveToStorage(conversations: ChatConversation[], activeId: string | null) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ conversations, activeId }))
  } catch { /* ignore */ }
}

export const useAiChatStore = defineStore('aiChat', () => {
  const saved = loadFromStorage()

  const conversations = ref<ChatConversation[]>(saved.conversations)
  const activeId = ref<string | null>(saved.activeId)

  // Auto-save on any state change
  watch(
    () => ({ conversations: conversations.value, activeId: activeId.value }),
    (state) => saveToStorage(state.conversations, state.activeId),
    { deep: true }
  )

  const activeConversation = computed(() => {
    if (!activeId.value) return null
    return conversations.value.find(c => c.id === activeId.value) || null
  })

  const sortedConversations = computed(() => {
    return [...conversations.value].sort((a, b) => b.createdAt - a.createdAt)
  })

  function generateId(): string {
    return 'conv-' + Date.now().toString(36) + '-' + Math.random().toString(36).substring(2, 8)
  }

  function createConversation(): string {
    const id = generateId()
    const conv: ChatConversation = {
      id,
      title: '新对话',
      messages: [],
      createdAt: Date.now(),
    }
    conversations.value.push(conv)
    activeId.value = id
    return id
  }

  function ensureConversation(): string {
    if (!activeId.value || !conversations.value.find(c => c.id === activeId.value)) {
      // If no active conversation or it was deleted, find the most recent or create new
      if (conversations.value.length > 0) {
        activeId.value = sortedConversations.value[0].id
        return activeId.value
      }
      return createConversation()
    }
    return activeId.value
  }

  function switchConversation(id: string) {
    if (conversations.value.find(c => c.id === id)) {
      activeId.value = id
    }
  }

  function deleteConversation(id: string) {
    const idx = conversations.value.findIndex(c => c.id === id)
    if (idx !== -1) {
      conversations.value.splice(idx, 1)
      if (activeId.value === id) {
        activeId.value = conversations.value.length > 0
          ? conversations.value[conversations.value.length - 1].id
          : null
      }
    }
  }

  function addMessage(msg: ChatMessage) {
    const conv = activeConversation.value
    if (!conv) return
    conv.messages.push(msg)
    // Auto-title: use first user message (truncated to 20 chars)
    if (conv.title === '新对话' && msg.role === 'user') {
      conv.title = msg.content.length > 20
        ? msg.content.substring(0, 20) + '...'
        : msg.content
    }
  }

  function clearActive() {
    const conv = activeConversation.value
    if (conv) {
      conv.messages = []
      conv.title = '新对话'
    }
  }

  return {
    conversations,
    activeId,
    activeConversation,
    sortedConversations,
    createConversation,
    ensureConversation,
    switchConversation,
    deleteConversation,
    addMessage,
    clearActive,
  }
})
