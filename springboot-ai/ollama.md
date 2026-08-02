# Ollama

I'll help you document the process of installing Ollama and setting up a lightweight LLM locally. Let me first fetch the current information from the Ollama website to ensure accuracy.
Here's a clear, step-by-step guide to install Ollama and run a lightweight LLM locally on Windows.

---

## Step 1: Install Ollama

Open **PowerShell** as Administrator and run:

```powershell
irm https://ollama.com/install.ps1 | iex
```

This downloads and installs Ollama on your system. The installer is provided directly by the official Ollama website.

---

## Step 2: Search for Llama Models

Once installed, visit the Ollama model library at:
**https://ollama.com/library**

Search for **"llama"** in the search bar. You'll find several Llama variants. For the **lowest parameter count** (best for lightweight local setup), look for:

| Model | Size | CLI Command |
|-------|------|-------------|
| **llama3.2:1b** | 1.3 GB | `ollama run llama3.2:1b` |
| **gemma3:270m** | Ultra-light | `ollama run gemma3:270m` |
| **qwen2.5:0.5b** | Very light | `ollama run qwen2.5:0.5b` |

The **Llama 3.2 1B** (`llama3.2:1b`) is the recommended pick — it's Meta's official 1B parameter model, genuinely useful for tool routing, classification, and simple chat tasks.

---

## Step 3: Download & Run the Model

Open **Command Prompt** or **PowerShell** and run:

```bash
ollama run llama3.2:1b
```

This will:
1. **Download** the model (~1.3 GB) to your local machine
2. **Load** it into memory
3. **Prompt** you to type your question

> **Note:** The download is a one-time process. Ollama stores models at `C:\Users\<YourUsername>\.ollama\models` on Windows.

---

## Step 4: Start Chatting

After the download completes, you'll see a prompt like:

```
>>> Send a message (/? for help)
```

Type your question and press **Enter**. For example:

```
>>> What is the capital of France?
```

The model will respond immediately since it's running entirely on your local machine — no internet required after the initial download.

---

## Useful Commands

| Command | Purpose |
|---------|---------|
| `ollama list` | Show all downloaded models |
| `ollama pull llama3.2:1b` | Download without running |
| `ollama rm llama3.2:1b` | Remove a model to free disk space |
| `ollama --version` | Check Ollama version |
| `/bye` | Exit the chat session |

---

## Quick Reference: Ultra-Lightweight Models

If you want something even smaller than 1B:

```bash
# 270M parameters — smallest useful model
ollama run gemma3:270m

# 500M parameters
ollama run qwen2.5:0.5b
```

These are good for basic classification and very simple tasks, but quality drops noticeably below 1B. 