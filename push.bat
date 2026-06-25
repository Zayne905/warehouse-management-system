@echo off
cd /d "%~dp0"

if not exist .git (
    git init
    git remote add origin https://github.com/Zayne905/warehouse-management-system.git
)

git fetch origin feature/ai-warehouse-assistant
git checkout feature/ai-warehouse-assistant 2>nul
git reset --mixed origin/feature/ai-warehouse-assistant

git add -A

echo === Changes to commit ===
git diff --cached --stat

git commit -m "feat: kanban QR lifecycle fixes, AI chat persistence, and various improvements"

echo === Pushing ===
git push origin feature/ai-warehouse-assistant
echo DONE
