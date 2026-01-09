#!/bin/bash
# Restart HealthHub AI agent after fixing Gemini model

echo "🔄 Restarting HealthHub AI Agent..."
echo "Password: openhabian"

sshpass -p openhabian ssh -o StrictHostKeyChecking=no openhabian@192.168.2.150 << 'EOF'
echo openhabian | sudo -S systemctl restart healthub-agent
sleep 3
systemctl status healthub-agent --no-pager -l
EOF

echo ""
echo "✅ Testing AI agent..."
curl -X POST http://192.168.2.150:8000/agent/interpret \
  -H "Content-Type: application/json" \
  -d '{"user_text":"hello","current_state":"Idle","session_id":"test123"}'
echo ""
