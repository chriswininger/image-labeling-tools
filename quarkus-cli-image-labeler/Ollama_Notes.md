# Ollama Notes

## Parallel Request Configuration

Ollama serializes requests to the same model by default. Two environment variables control parallelism:

### OLLAMA_NUM_PARALLEL

Controls how many requests a single loaded model can handle concurrently.

- **Default:** 1
- **Effect:** Increasing this allows multiple requests to be processed simultaneously on the same model instance.
  The model weights are loaded once in VRAM, and each parallel slot allocates its own KV cache with the full
  context length. Per the Ollama FAQ, required RAM scales by `OLLAMA_NUM_PARALLEL * CONTEXT_LENGTH`.
- **Tradeoff:** Higher values require more VRAM. On consumer GPUs, values of 2-3 are a reasonable starting point.

### OLLAMA_MAX_LOADED_MODELS

Controls how many different models can be loaded in memory simultaneously.

- **Default:** 1 (3 if `OLLAMA_NUM_PARALLEL` is set to a value > 1, though this may vary by version)
- **Effect:** If your pipeline uses multiple models (e.g., one for vision, one for OCR), this allows
  them to stay loaded rather than swapping in and out.

### Setting These Variables (systemd)

If Ollama is running as a systemd service, create an override:

```bash
sudo systemctl edit ollama
```

Add the configuration in the editor:

```
[Service]
Environment="OLLAMA_NUM_PARALLEL=3"
```

Then reload and restart:

```bash
sudo systemctl daemon-reload && sudo systemctl restart ollama
```

Verify the setting took effect:

```bash
systemctl show ollama --property=Environment
```

## Context Window and Parallelism

### "Context sharing" does not mean shared conversation context

When `OLLAMA_NUM_PARALLEL` is set, each request gets its own independent context window with the full context
length. Requests do not see each other's prompts or outputs. Each parallel slot allocates its own KV cache, so
total VRAM usage scales by `OLLAMA_NUM_PARALLEL * CONTEXT_LENGTH`. This means higher parallelism requires more
VRAM, but does not reduce per-request context.

### What `ollama ps` shows

Ollama seems to account for OLLAMA_NUM_PARALLEL by increasing the memory allocation but it doesn't show it in the way
you would expect. The CONTEXT still shows 4096 which is the default, but you'll notice SIZE goes up. CONTEXT appears to
show the context given to each request.

Default OLLAMA_NUM_PARALLEL=1:
```
NAME         ID              SIZE      PROCESSOR    CONTEXT    UNTIL
gemma3:4b    a2af6cc3eb7f    4.4 GB    100% GPU     4096       4 minutes from now
```

OLLAMA_NUM_PARALLEL=3
```
NAME         ID              SIZE      PROCESSOR    CONTEXT    UNTIL
gemma3:4b    a2af6cc3eb7f    4.8 GB    100% GPU     4096       4 minutes from now
```

- **SIZE** reflects total memory used including model weights and all KV caches. This will increase when
  `OLLAMA_NUM_PARALLEL` is raised (e.g., 4.4GB at parallel=1 vs 4.8GB at parallel=3).
- **CONTEXT** is per-request, not a shared budget. With `OLLAMA_NUM_PARALLEL=3` and a context of 4096, each
  request gets the full 4096 tokens. Per the Ollama FAQ, required RAM scales by `OLLAMA_NUM_PARALLEL * CONTEXT_LENGTH`.
- Ollama may allocate less context than the model's maximum (e.g., 4096 out of a supported 131072) based on available VRAM after loading model weights.

### Performance expectations

Parallelism on a single GPU improves throughput through better GPU utilization, but gains are modest compared to the parallelism factor. In testing, `OLLAMA_NUM_PARALLEL=3` yielded roughly a 35% speedup rather than 3x, since inference is GPU-compute bound rather than I/O bound.
