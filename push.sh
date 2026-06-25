#!/bin/bash
cd /c/Users/zhanghaoming/Desktop/warehouse-management-system-feature-ai-warehouse-assistant

# Ensure git is initialized
if [ ! -d .git ]; then
  git init
  git remote add origin https://github.com/Zayne905/warehouse-management-system.git
  git fetch origin feature/ai-warehouse-assistant
fi

# Checkout or create branch
git checkout -b feature/ai-warehouse-assistant 2>/dev/null || git checkout feature/ai-warehouse-assistant

# Merge remote changes first to avoid conflicts
git reset --mixed origin/feature/ai-warehouse-assistant 2>/dev/null

# Stage all changes
git add -A

echo "=== Changes to commit ==="
git diff --cached --stat

# Commit
git commit -m "feat: kanban QR lifecycle fixes, AI chat persistence, and various improvements

- AI Chat: persist conversations across tab switches, add history drawer and new chat button
- Kanban: incremental update on order edit, stable kanbanNo using order create date
- Kanban: add qr_content column to persist QR code, ensuring lifecycle uniqueness
- Kanban: fix thread-safe kanban number generation in RepackService and OutboundService
- Kanban: validate kanban belongs to correct order during scan
- Kanban: validate kanban status before inbound scan
- Scanner: fix quantity type from Int to Double to support decimal quantities
- Scanner: fix kanban management QR to use same JSON format as printed labels
- Print: remove frontend kanban fallback generation, use DB kanbans only
- Print: all QR displays now use stored qrContent
- Inbound: remove CANCELLED status from inbound orders
- Inbound: change box count step from 0.10 to 1.00
- DB: add V15 kanban qr_content migration
- Various SQL migrations applied (V4-V14)"

echo "=== Pushing ==="
git push origin feature/ai-warehouse-assistant 2>&1
echo "DONE"
