# IT Support Request: Azure OpenAI Public Access

**Date:** 23 December 2025  
**Requestor:** [Your Name]  
**Project:** HealthHub - Healthcare Robot AI Integration (Research Prototype)  
**Azure Resource:** openai-nr-1625688

---

## Issue Summary

Unable to access Azure OpenAI API from external networks despite configuring firewall rules. Receiving **403 "Public access is disabled"** error even after adding IP allowlist.

---

## What I've Tried

1. ✅ Added my public IP (109.146.14.59) to firewall allowlist
2. ✅ Selected "All networks" in networking settings
3. ✅ Verified API key is valid
4. ✅ Saved all configuration changes
5. ❌ Still receiving 403 error: "Public access is disabled. Please configure private endpoint"

---

## Request

**Enable public network access for Azure OpenAI resource: `openai-nr-1625688`**

**Specific requirements:**
- Allow API access from IP: `109.146.14.59` (development workstation)
- Allow API access from IP: `192.168.2.150` (Raspberry Pi - when on-site)
- Allow API access from IP: `192.168.2.115` (Temi robot - when on-site)

---

## Business Justification

**Project:** Research prototype for healthcare screening robot  
**Purpose:** Voice-based patient questionnaire using AI  
**Environment:** Development/testing only (not production)  
**Duration:** Research project (academic/prototype)  
**Data:** No real patient data (test data only)  
**Security:** API keys will be rotated after development phase

---

## Technical Details

**Endpoint:** `https://openai-nr-1625688.openai.azure.com`  
**Deployment:** gpt-4 (or gpt-4.1)  
**API Version:** 2024-02-15-preview  
**Error Code:** 403  
**Error Message:** "Public access is disabled. Please configure private endpoint"

---

## Alternative Solutions Considered

1. **Private Endpoint:** Too complex for research prototype, requires VNet setup
2. **VPN Access:** Not available for external development
3. **Third-party AI (Groq):** Currently using as workaround, but prefer Azure for institutional compliance

---

## Urgency

**Medium** - Currently blocked on AI integration testing. Using temporary workaround (Groq API) but would prefer Azure OpenAI for:
- Institutional compliance
- Data governance
- Long-term sustainability

---

## Expected Outcome

After enabling public access with IP allowlist, the following API call should succeed:

```bash
curl -X POST "https://openai-nr-1625688.openai.azure.com/openai/deployments/gpt-4/chat/completions?api-version=2024-02-15-preview" \
  -H "Content-Type: application/json" \
  -H "api-key: [API_KEY]" \
  -d '{"messages": [{"role": "user", "content": "test"}], "max_tokens": 10}'
```

**Current result:** 403 error  
**Expected result:** JSON response with AI completion

---

## Contact Information

**Name:** [Your Name]  
**Email:** [Your Email]  
**Department:** [Your Department]  
**Project Supervisor:** [If applicable]

---

## Additional Notes

This is a **research prototype** for healthcare robotics. The system architecture is designed to be backend-agnostic (can switch between Azure OpenAI, Groq, or other AI services via configuration). Azure OpenAI is preferred for institutional alignment, but not a hard requirement.

If enabling public access violates organizational policy, please advise on alternative solutions (VPN, private endpoint, or approval for third-party AI service).

---

**Thank you for your assistance!**
