# Azure OpenAI API Error Log

## Test Details
- Date: 2025-12-23
- Endpoint: https://openai-nr-1625688.openai.azure.com
- Deployment: gpt-4
- API Version: 2024-02-15-preview
- Source IP: 109.146.14.59 (added to firewall allowlist)

## Error Response

```json
{
  "error": {
    "code": "403",
    "message": "Public access is disabled. Please configure private endpoint."
  }
}
```

## HTTP Details

**Request:**
```
POST /openai/deployments/gpt-4/chat/completions?api-version=2024-02-15-preview
Host: openai-nr-1625688.openai.azure.com
Content-Type: application/json
api-key: [VALID_API_KEY]

Body:
{
  "messages": [{"role": "user", "content": "test"}],
  "max_tokens": 10
}
```

**Response:**
```
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "error": {
    "code": "403",
    "message": "Public access is disabled. Please configure private endpoint."
  }
}
```

## Configuration Attempted

1. Added source IP (109.146.14.59) to Azure firewall allowlist
2. Selected "All networks" in networking settings
3. Verified API key is valid (Key 1)
4. Saved all configuration changes

## Issue

Despite adding IP to allowlist, the API returns 403 error indicating public access is disabled at the organizational policy level.

## Request

Enable public network access for Azure OpenAI resource: openai-nr-1625688

Required IP addresses:
- 109.146.14.59 (development workstation)
- 192.168.2.150 (Raspberry Pi - when on-site)
- 192.168.2.115 (Temi robot - when on-site)
