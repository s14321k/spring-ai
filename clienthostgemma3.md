# Docker Model Runner & Spring AI Troubleshooting Guide

## Environment

### Operating System

* Windows 11

### Docker

* Docker Desktop 4.83.0
* Docker Engine 29.6.2

### AI Framework

* Spring AI 2.0

### Local AI Runtime

* Docker Model Runner
* Ollama

### GPU

* NVIDIA RTX 3050 Laptop
* 4 GB VRAM

---

# 1. Verify Installed Docker Models

## Command

```bash
docker model ls
```

### Purpose

Lists all AI models installed in Docker Model Runner.

### Sample Output

```text
MODEL NAME  PARAMETERS  QUANTIZATION   ARCHITECTURE
gemma3      3.88 B      MOSTLY_Q4_K_M  gemma3
```

---

# 2. Inspect Model Information

## Command

```bash
docker model inspect ai/gemma3
```

### Purpose

Displays detailed metadata about the model.

Useful information:

* Architecture
* Parameter count
* Quantization
* Context length
* GGUF metadata

Important finding:

```text
gemma3.context_length = 131072
```

This extremely large context consumes a significant amount of GPU memory.

---

# 3. Check Running Models

## Command

```bash
docker model ps
```

### Purpose

Shows currently loaded models.

Example

```text
MODEL NAME  BACKEND    MODE
gemma3      llama.cpp  completion
```

---

# 4. Test Docker Model Directly

## Command

```bash
docker model run ai/gemma3 "hi"
```

### Purpose

Verifies that Docker can load the model without involving Spring AI.

---

# 5. Verify OpenAI Compatible API

## List Models

```bash
curl http://localhost:12434/engines/v1/models
```

Purpose:

Verify that the OpenAI compatible endpoint is working.

Expected response:

```json
{
  "object":"list",
  "data":[...]
}
```

---

## Chat Completion

```bash
curl -X POST http://localhost:12434/engines/v1/chat/completions ^
-H "Content-Type: application/json" ^
-H "Authorization: Bearer dummy" ^
-d "{\"model\":\"ai/gemma3\",\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]}"
```

Purpose:

Verify whether the model can generate responses.

Observed Result

```json
{
    "error": {
        "code":500,
        "message":"decode() failed: vk::Device::allocateMemory: ErrorOutOfDeviceMemory"
    }
}
```

Conclusion:

Problem exists inside Docker Model Runner, not Spring AI.

---

# 6. Verify GPU Usage

## Command

```bash
nvidia-smi
```

Purpose

Displays

* GPU model
* Driver version
* CUDA version
* VRAM usage
* Running GPU processes

Observed

```text
Memory Usage

3866 MiB / 4096 MiB
```

Only ~230 MB remained.

---

# 7. Identify GPU Processes

Observed

```
com.docker.llama-server.exe
Postman.exe
```

Initially

```
ollama.exe
llama-server.exe
com.docker.llama-server.exe
```

Both Ollama and Docker Model Runner were occupying GPU memory.

---

# 8. Stop Ollama

Commands

```cmd
taskkill /F /IM ollama.exe
```

```cmd
taskkill /F /IM llama-server.exe
```

Purpose

Free GPU memory.

Verification

```bash
nvidia-smi
```

Confirmed only Docker Model Runner remained.

---

# 9. List Ollama Models

```bash
ollama list
```

Purpose

Display downloaded Ollama models.

---

# 10. Remove Ollama Model

```bash
ollama rm llama3.2:1b
```

or

```bash
ollama rm llama3.2:latest
```

Purpose

Delete downloaded models.

---

# 11. Remove Docker Model

```bash
docker model rm ai/gemma3
```

Purpose

Delete Docker Model Runner model.

---

# 12. Check Docker Disk Usage

```bash
docker system df
```

Purpose

Shows

* Images
* Volumes
* Containers
* Space consumed

---

# 13. Verify Listening Ports

```cmd
netstat -an | findstr 12434
```

Purpose

Determine whether Docker Model Runner is listening.

Observed

```text
TCP 0.0.0.0:12434 LISTENING
```

Meaning

TCP socket is open.

This does NOT guarantee HTTP requests from remote devices are accepted.

---

# 14. Test LAN Connectivity

Local

```bash
curl http://localhost:12434/engines/v1/models
```

Result

✅ Works

Remote (same PC)

```bash
curl http://192.168.31.232:12434/engines/v1/models
```

Result

```
curl: (52) Empty reply from server
```

Remote (Mobile)

```bash
curl http://192.168.31.232:12434/engines/v1/models
```

Result

```
Empty reply from server
```

Conclusion

Docker Model Runner accepts localhost requests but closes LAN HTTP requests.

---

# 15. Verify Spring Boot

From mobile

```bash
curl "http://192.168.31.232:8080/chat?message=hello"
```

Result

✅ Successful

Conclusion

Network and Spring Boot configuration are correct.

---

# 16. Spring AI Configuration

```properties
spring.ai.openai.chat.base-url=http://localhost:12434/engines/v1
spring.ai.openai.chat.model=ai/gemma3
spring.ai.openai.api-key=dummy
```

Recommendation

Use localhost instead of LAN IP because Spring Boot and Docker Model Runner run on the same machine.

---

# Root Cause Analysis

Issue 1

```
vk::Device::allocateMemory: ErrorOutOfDeviceMemory
```

Cause

RTX 3050 Laptop has only 4 GB VRAM.

Gemma 3 4B requires more GPU memory during inference than is available.

---

Issue 2

```
Empty reply from server
```

Cause

Docker Model Runner currently serves requests correctly only through localhost.

Requests using the LAN IP establish a TCP connection but the HTTP connection is closed immediately without a response.

This behavior was confirmed by:

* localhost → Works
* LAN IP (same PC) → Empty reply
* LAN IP (mobile) → Empty reply

Therefore the issue is not:

* Spring AI
* Windows Firewall
* Mobile phone
* Wi-Fi network

The limitation is in Docker Model Runner's current networking behavior.

---

# Recommended Architecture

```
Android App
        │
        ▼
Spring Boot (8080)
        │
        ▼
localhost:12434
        │
        ▼
Docker Model Runner
```

Do not expose Docker Model Runner directly to clients.

Expose only the Spring Boot API.

Benefits

* Better security
* Authentication support
* Logging
* Rate limiting
* Stable architecture
* Hides AI infrastructure from clients

---

# Useful Commands Summary

```bash
docker model ls
docker model ps
docker model inspect ai/gemma3
docker model run ai/gemma3 "hi"
docker model rm ai/gemma3

ollama list
ollama rm llama3.2:1b
ollama rm llama3.2:latest

docker system df

nvidia-smi

netstat -an | findstr 12434

curl http://localhost:12434/engines/v1/models

curl http://192.168.31.232:12434/engines/v1/models

curl "http://192.168.31.232:8080/chat?message=hello"
```
