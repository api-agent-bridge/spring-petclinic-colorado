# Petclinic evals

These evals ask local models the questions of [`docs/mcp-queries.md`](../docs/mcp-queries.md) and [`docs/mcp-mutations.md`](../docs/mcp-mutations.md) through the Petclinic MCP server, and check the answers and the tool calls. They run on [promptfoo](https://www.promptfoo.dev) against one Ollama model from each maker, listed in `models.yaml` with the reasons for the choice.

## Why there is a provider of our own

promptfoo's own MCP support, the `mcp:` block of its OpenAI provider, runs one round. It calls the tools of the model's first answer and returns their results as the output, and those results stay out of the conversation with the model. That is enough to check which tool a model picks first.

Most Petclinic questions need a search and then the details, so `mcp-agent.mjs` runs the loop an agent runs. It sends the question with the tools from the server's `tools/list`, calls every tool the model asks for through MCP, sends the results back, and stops when the model answers in text, or after 25 rounds. The models run in Ollama, through its own chat endpoint at `http://localhost:11434/api/chat`.

## How the models run

Every model runs the way Ollama ships it, the way `ollama run` runs it, and the evals leave its settings as they are:

- **Sampling.** The provider leaves the sampling options out of its requests. Ollama starts from its defaults (temperature 0.8, top_k 40, top_p 0.9) and applies the parameters of the model's Modelfile over them, which `ollama show --parameters <model>` lists. Ollama's OpenAI-compatible endpoint, `/v1/chat/completions`, sets temperature 1.0 and top_p 1.0 when a request leaves them out (`openai/openai.go` in Ollama 0.32.15), so the provider calls `/api/chat` instead.
- **Thinking.** Ollama turns thinking on for every model that supports it ([docs](https://docs.ollama.com/capabilities/thinking)). Each answer goes back into the conversation with its thinking, as Ollama's [tool calling docs](https://docs.ollama.com/capabilities/tool-calling) do it.
- **Context.** On a Mac with more than 48 GiB of GPU memory, Ollama gives each model 262,144 tokens of context, or the model's maximum where that is smaller ([docs](https://docs.ollama.com/context-length)). The `CONTEXT` column of `ollama ps` shows it. The largest request of a full run was 6,410 tokens.
- **Quantization.** The default Ollama tag of each model: `qwen3.5:0.8b` is 8-bit, `gpt-oss:20b` is MXFP4, and the other nine are 4-bit (`Q4_K_M`).
- **System prompt.** Ours says the assistant works for a veterinary clinic and gives today's date. It replaces the system prompt a model brings, such as Ornith's "agentic coding assistant" or Mistral's default one.
- **Loading.** The provider loads the model before the 60 seconds of a question start, so a large model keeps all of its time for the question. The grid shows the loading time in a column of its own.

promptfoo's [guide for comparing open models](https://www.promptfoo.dev/docs/guides/compare-open-source-models/) runs every model at temperature 0.01. We run each one at its own settings instead, because that is how people use them.

Where the Modelfile leaves a value out, the model runs at Ollama's default, and for four models that differs from what their maker recommends:

| Model | Ollama runs it with | The maker recommends | Source |
| --- | --- | --- | --- |
| `qwen3.5:0.8b` | temperature 1, top_k 20, top_p 0.95, presence_penalty 1.5 | the same, for thinking mode | [Qwen](https://huggingface.co/Qwen/Qwen3.5-0.8B) |
| `lfm2.5:8b` | temperature 0.2, top_k 80, repeat_penalty 1.05, top_p 0.9 | the same, without top_p | [Liquid AI](https://huggingface.co/LiquidAI/LFM2.5-8B-A1B) |
| `granite4.1:8b` | temperature 0.8, top_k 40, top_p 0.9 | IBM does not publish a recommendation | [IBM](https://huggingface.co/ibm-granite/granite-4.1-8b) |
| `ornith:9b` | temperature 0.6, top_k 20, top_p 0.95 | the same | [Ornith](https://huggingface.co/ornith-ai/Ornith-1.0-9B) |
| `ministral-3:14b` | temperature 0.15, top_k 40, top_p 0.9 | temperature 0.15 in the card's code, "below 0.1" in its text | [Mistral AI](https://huggingface.co/mistralai/Ministral-3-14B-Instruct-2512) |
| `gpt-oss:20b` | temperature 1, top_k 40, top_p 0.9 | temperature 1.0, top_p 1.0 | [OpenAI](https://github.com/openai/gpt-oss#recommended-sampling-parameters) |
| `qwen3.8:27b` | temperature 1, top_k 20, top_p 0.95, min_p 0 | the same, for thinking mode | [Qwen](https://huggingface.co/Qwen/Qwen3.8-27B) |
| `gemma4:26b` | temperature 1, top_k 64, top_p 0.95 | the same | [Google](https://huggingface.co/google/gemma-4-26B-A4B-it) |
| `glm-4.7-flash` | temperature 1, top_k 40, top_p 0.95, min_p 0.01 | temperature 1.0, top_p 0.95 | [Z.ai](https://huggingface.co/zai-org/GLM-4.7-Flash) |
| `laguna-xs-2.1` | temperature 0.8, top_k 40, top_p 0.9 | temperature 1.0, top_k 20, top_p 1.0 | [Poolside](https://huggingface.co/poolside/Laguna-XS-2.1) |
| `nemotron3:33b` | temperature 0.8, top_k 40, top_p 0.9 | temperature 0.6, top_p 0.95 | [NVIDIA](https://huggingface.co/nvidia/Nemotron-3-Nano-Omni-30B-A3B-Reasoning-BF16) |

A `temperature` in the `config` of a model in `models.yaml` overrides its own, for a run that wants the maker's settings.

## What is checked

Every run gets two marks, because a model can get one right and the other wrong:

| Mark | Metric | Checked by | What it means |
| --- | --- | --- | --- |
| T | `tools` | `assert-tools.mjs` | The agent called the right tools with the right arguments, left alone the tools it had to leave alone, and answered without tools where that is right |
| A | `answer` | `icontains-all`, `regex`, `not-regex` and a few short JavaScript checks in the configs | The answer holds the facts of the expected answer and only true claims. Opening hours the tools do not hold fail it, and so does a reply that the owner was deleted |

A run passes only when both marks pass. Its score shows the half that passed, so a model that answers "What are the clinic's opening hours?" without a tool call and then invents the hours fails with a score of 0.5, and the grid shows `T1/1 A0/1`. A write question that is judged by its calls alone shows `A-`.

The tool mark reads like this for each kind of question:

- A read question needs a call of a tool that holds its data, so an answer made up without tools fails. Where two tools hold the data, such as the pets in `findPets` and in `getOwnerDetails`, either one counts, and the calls grid shows what each way costs. For the address of George Franklin and the visits of Jean Coleman, the check also reads the id the agent passed to `getOwnerDetails`, so a call with the id `"Jean Coleman"` fails.
- A read question fails when the agent calls a tool that writes. GATool marks the five write tools with `readOnlyHint: false`, and on the read questions the provider declines their calls, the way a client that asks before a write answers when the user says no. The model reads "The user declined the call of updatePet.", and the data stays the same for the runs after it. Without it, `qwen3.5:0.8b` answered "Which pets have not visited the clinic yet?" by renaming three pets to "Orphan Pet No-Visit", and the models after it answered from the renamed pets.
- A write question checks the write, how often it happened, and the arguments of its first call.
- The last three read questions pass only without a tool call: a question about a capital city, a greeting, and the opening hours.
- Three write questions pass only when the agent leaves every write alone: two because it has to ask the user first, and one because it asks for a delete that the tools cannot do.

## What a run records

Every run keeps the answer and, in its metadata, everything the agent did on the way:

- each tool call with its arguments, its result, whether it failed, and for a write tool `writes: true`, with `declined: true` when the provider declined it
- each round with the model: its time, the part of it Ollama spent loading the model, its prompt and completion tokens, the tools it asked for, and the reasoning of a thinking model
- the time spent in the model, the part of it spent loading the model, the time spent in the tools, and the characters of all tool results together

Five named scores sit on every run, and `npm run view` averages them for each model: `tools` and `answer` (the two marks), `toolCalls`, `rounds` (requests to the model, the last one being the answer) and `repeatedCalls` (calls with the same tool and the same arguments as an earlier call). Click a cell for the answer, the reason of each check and the metadata.

`npm run grid` prints the same results in the terminal: the two marks for each question and model, the tool calls and rounds for each question and model, and a table for each model with runs passed, tools right, answers right, provider errors, prompt and completion tokens, total time, time loading the model, time in the model and in the tools, rounds, tool calls, repeated calls, and characters of tool results.

## Running

Start the app with fresh data. GATool allows each caller 60 calls of one tool a minute, and the rabies booster question reads every owner, calling `getOwnerDetails` ten times in a few seconds, so raise the limit for the evals:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--gatool.mcp.rate-limit.calls-per-minute=10000
```

Then, in this folder:

```bash
npm install

# every read question on every model
npm run queries

# restart the app first, then every write question on every model
npm run mutations

# read the results
npm run grid -- results/queries.json   # pass grid and tool calls, in the terminal
npm run view                           # the grid in a browser, with every answer and call
```

`MCP_URL` and `OLLAMA_URL` point the provider at another server or another Ollama.

`--no-cache` is in every command because promptfoo caches answers by default, and a cached answer hides how a model behaves on the next run. Each model runs at the temperature its maker ships, 1.0 for most of them, so the answers vary from run to run, and `--repeat 3` separates "always wrong" from "sometimes wrong", at three times the time.

## How long it takes

The read questions on the 11 models take about 30 minutes, and about 90 with `--repeat 3`.

The agent has 60 seconds for each question, counted from the moment the model is in memory. A model that runs out of time ends the run with an error, which the grid shows as `E1`. `timeoutSeconds` in the `config` of a provider changes the limit. `promptfoo view` keeps every run in `~/.promptfoo` and works without an account or a network.

## The writes change the data

Every model writes to the same app. After the first model, Harold Davis already lives at 1701 Wynkoop St and Bobbie is already registered, so later models meet a `name: is already in use` error on question m3. The checks look at the calls, which stay the same, and the grid shows what each model does with the error. Restart the app before each run of `mutations.yaml`, and before `queries.yaml` if the writes ran first: a new dog changes the answer to "Which dogs are registered at the clinic?".

## Files

| File | What it is |
| --- | --- |
| `mcp-agent.mjs` | The promptfoo provider that runs the agent loop over MCP |
| `models.yaml` | The 11 local models, smallest first |
| `queries.yaml` | The sixteen read questions with their checks |
| `mutations.yaml` | The seven write questions with their checks |
| `assert-tools.mjs` | The check of the tool calls |
| `assert-no-visits.mjs` | The answer check of "Which pets have not visited the clinic yet?", which accepts a list and an exception |
| `summarise.mjs` | Collapses repeats into a pass grid and a grid of tool calls |

promptfoo 0.123.1 and the MCP SDK 1.30.0 are pinned to exact versions.
